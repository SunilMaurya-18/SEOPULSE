package com.seopulse.organization.service;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.auth.service.SecureTokens;
import com.seopulse.billing.EntitlementService;
import com.seopulse.common.exception.InvalidStateException;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.notification.EmailOutboxService;
import com.seopulse.notification.EmailTemplates;
import com.seopulse.organization.entity.Invitation;
import com.seopulse.organization.entity.OrgAuditLog;
import com.seopulse.organization.entity.Organization;
import com.seopulse.organization.entity.OrganizationMember;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.repository.InvitationRepository;
import com.seopulse.organization.repository.OrgAuditLogRepository;
import com.seopulse.organization.repository.OrganizationMemberRepository;
import com.seopulse.organization.repository.OrganizationRepository;
import com.seopulse.user.entity.User;
import com.seopulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final InvitationRepository invitationRepository;
    private final OrgAuditLogRepository auditLogRepository;
    private final OrganizationAccessService accessService;
    private final OrganizationProvisioningService provisioningService;
    private final EntitlementService entitlementService;
    private final UserRepository userRepository;
    private final EmailOutboxService emailOutboxService;
    private final AuthProperties authProperties;

    @Transactional(readOnly = true)
    public List<OrganizationMember> listForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        provisioningService.ensureFor(user);
        return memberRepository.findByUserIdOrderByIdAsc(userId);
    }

    public Organization create(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String slug = uniqueSlug(OrganizationProvisioningService.slugify(name));
        Organization organization = organizationRepository.save(Organization.builder()
                .name(name.trim())
                .slug(slug)
                .build());
        memberRepository.save(OrganizationMember.builder()
                .organization(organization)
                .user(user)
                .role(OrganizationRole.OWNER)
                .build());
        provisioningService.attachFreePlan(organization);
        record(organization.getId(), userId, "ORG_CREATED", organization.getName());
        return organization;
    }

    public Organization rename(Long organizationId, Long userId, String name) {
        accessService.requireRole(organizationId, userId, OrganizationRole.ADMIN);
        Organization organization = organization(organizationId);
        organization.setName(name.trim());
        record(organizationId, userId, "ORG_RENAMED", organization.getName());
        return organization;
    }

    public void delete(Long organizationId, Long userId) {
        accessService.requireRole(organizationId, userId, OrganizationRole.OWNER);
        record(organizationId, userId, "ORG_DELETED", null);
        organizationRepository.deleteById(organizationId);
    }

    @Transactional(readOnly = true)
    public List<OrganizationMember> members(Long organizationId, Long userId) {
        accessService.requireRole(organizationId, userId, OrganizationRole.VIEWER);
        return memberRepository.findByOrganizationIdOrderByIdAsc(organizationId);
    }

    @Transactional(readOnly = true)
    public List<Invitation> invitations(Long organizationId, Long userId) {
        accessService.requireRole(organizationId, userId, OrganizationRole.ADMIN);
        return invitationRepository.findByOrganizationIdAndAcceptedAtIsNullAndRevokedAtIsNull(organizationId);
    }

    public void changeRole(Long organizationId, Long actorId, Long memberUserId, OrganizationRole role) {
        accessService.requireRole(organizationId, actorId, OrganizationRole.ADMIN);
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        guardLastOwner(organizationId, member, role);
        member.setRole(role);
        record(organizationId, actorId, "ROLE_CHANGED", memberUserId + " -> " + role);
    }

    public void removeMember(Long organizationId, Long actorId, Long memberUserId) {
        accessService.requireRole(organizationId, actorId, OrganizationRole.ADMIN);
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        guardLastOwner(organizationId, member, null);
        memberRepository.delete(member);
        record(organizationId, actorId, "MEMBER_REMOVED", String.valueOf(memberUserId));
    }

    public void leave(Long organizationId, Long userId) {
        OrganizationMember member = accessService.requireRole(organizationId, userId, OrganizationRole.VIEWER);
        guardLastOwner(organizationId, member, null);
        memberRepository.delete(member);
        record(organizationId, userId, "MEMBER_LEFT", null);
    }

    public Invitation invite(Long organizationId, Long actorId, String email, OrganizationRole role) {
        accessService.requireRole(organizationId, actorId, OrganizationRole.ADMIN);
        if (role == OrganizationRole.OWNER) {
            throw new InvalidStateException("Transfer ownership is not available from an invite");
        }
        entitlementService.checkMemberCapacity(organizationId);
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        String token = SecureTokens.generate();
        Invitation invitation = invitationRepository.save(Invitation.builder()
                .organization(organization(organizationId))
                .email(normalized)
                .role(role)
                .tokenHash(SecureTokens.sha256Hex(token))
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build());
        String url = authProperties.getAppBaseUrl() + "/settings?invite=" + token;
        emailOutboxService.enqueue(
                normalized,
                "You're invited to an SEOPulse workspace",
                EmailTemplates.text("Join the workspace to review audits and reports.", url),
                EmailTemplates.html("Workspace invitation", "Join the workspace to review audits and reports.", "Accept invite", url)
        );
        record(organizationId, actorId, "INVITE_SENT", normalized);
        return invitation;
    }

    public void revokeInvite(Long organizationId, Long actorId, Long invitationId) {
        accessService.requireRole(organizationId, actorId, OrganizationRole.ADMIN);
        Invitation invitation = invitationRepository.findById(invitationId)
                .filter(item -> item.getOrganization().getId().equals(organizationId))
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        invitation.setRevokedAt(Instant.now());
        record(organizationId, actorId, "INVITE_REVOKED", invitation.getEmail());
    }

    public void accept(Long userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Invitation invitation = invitationRepository.findByTokenHash(SecureTokens.sha256Hex(token))
                .filter(item -> item.isOpen(Instant.now()))
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new InvalidStateException("This invitation was sent to a different email address");
        }
        if (memberRepository.findByOrganizationIdAndUserId(invitation.getOrganization().getId(), userId).isEmpty()) {
            memberRepository.save(OrganizationMember.builder()
                    .organization(invitation.getOrganization())
                    .user(user)
                    .role(invitation.getRole())
                    .build());
        }
        invitation.setAcceptedAt(Instant.now());
        record(invitation.getOrganization().getId(), userId, "INVITE_ACCEPTED", user.getEmail());
    }

    private void guardLastOwner(Long organizationId, OrganizationMember member, OrganizationRole next) {
        if (member.getRole() != OrganizationRole.OWNER) {
            return;
        }
        if (next == OrganizationRole.OWNER) {
            return;
        }
        if (memberRepository.countByOrganizationIdAndRole(organizationId, OrganizationRole.OWNER) <= 1) {
            throw new InvalidStateException("The last owner cannot leave or be demoted");
        }
    }

    private Organization organization(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    private String uniqueSlug(String base) {
        String slug = base;
        int suffix = 2;
        while (organizationRepository.findBySlug(slug).isPresent()) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private void record(Long organizationId, Long actorId, String action, String detail) {
        auditLogRepository.save(OrgAuditLog.builder()
                .organizationId(organizationId)
                .actorUserId(actorId)
                .action(action)
                .detail(detail)
                .build());
    }

}
