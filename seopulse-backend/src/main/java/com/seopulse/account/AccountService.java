package com.seopulse.account;

import com.seopulse.billing.EntitlementService;
import com.seopulse.common.exception.InvalidStateException;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.newsletter.NewsletterSubscriberRepository;
import com.seopulse.notification.EmailOutboxService;
import com.seopulse.notification.EmailTemplates;
import com.seopulse.organization.entity.OrgAuditLog;
import com.seopulse.organization.entity.OrganizationMember;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.repository.OrgAuditLogRepository;
import com.seopulse.organization.repository.OrganizationMemberRepository;
import com.seopulse.organization.service.OrganizationPurger;
import com.seopulse.project.entity.Project;
import com.seopulse.project.repository.ProjectRepository;
import com.seopulse.user.entity.User;
import com.seopulse.user.repository.UserRepository;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.Website;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.repository.WebsiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationMemberRepository memberRepository;
    private final OrgAuditLogRepository auditLogRepository;
    private final OrganizationPurger organizationPurger;
    private final ProjectRepository projectRepository;
    private final WebsiteRepository websiteRepository;
    private final AuditRepository auditRepository;
    private final EntitlementService entitlementService;
    private final NewsletterSubscriberRepository newsletterRepository;
    private final EmailOutboxService emailOutboxService;

    @Transactional(readOnly = true)
    public AccountExport export(Long userId) {
        User user = user(userId);
        List<AccountExport.Workspace> workspaces = memberRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(this::workspace)
                .toList();
        AccountExport.Newsletter newsletter = newsletterRepository.findByEmail(user.getEmail())
                .map(subscriber -> new AccountExport.Newsletter(
                        subscriber.isActive(),
                        subscriber.getConsentAt(),
                        subscriber.getConfirmedAt(),
                        subscriber.getUnsubscribedAt()))
                .orElse(null);
        return new AccountExport(
                Instant.now(),
                new AccountExport.Profile(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getCreatedAt(),
                        user.getEmailVerifiedAt(),
                        user.getTermsAcceptedVersion(),
                        user.getTermsAcceptedAt()),
                workspaces,
                newsletter
        );
    }

    /**
     * Deletes the user. Workspaces nobody else belongs to are deleted with
     * all their data and any paid subscription is cancelled. In shared
     * workspaces the user's projects pass to the most senior teammate.
     */
    @Transactional
    public void delete(Long userId, String password) {
        User user = user(userId);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Password is incorrect");
        }

        List<OrganizationMember> memberships = memberRepository.findByUserIdOrderByIdAsc(userId);
        requireSuccessorOwners(userId, memberships);

        Set<Long> organizationIds = new LinkedHashSet<>();
        memberships.forEach(member -> organizationIds.add(member.getOrganization().getId()));
        organizationIds.addAll(projectRepository.findOrganizationIdsByUserId(userId));

        for (Long organizationId : organizationIds) {
            List<OrganizationMember> others = memberRepository.findByOrganizationIdOrderByIdAsc(organizationId).stream()
                    .filter(member -> !member.getUser().getId().equals(userId))
                    .sorted(Comparator.comparing((OrganizationMember member) -> member.getRole().ordinal()).reversed())
                    .toList();
            if (others.isEmpty()) {
                organizationPurger.purge(organizationId);
                continue;
            }
            projectRepository.reassignCreator(userId, others.getFirst().getUser().getId(), organizationId);
            auditLogRepository.save(OrgAuditLog.builder()
                    .organizationId(organizationId)
                    .actorUserId(userId)
                    .action("ACCOUNT_DELETED")
                    .build());
        }

        String email = user.getEmail();
        newsletterRepository.deleteByEmail(email);
        userRepository.deleteById(userId);

        String intro = "Your SEOPulse account and the workspaces only you belonged to have been deleted. "
                + "If you did not do this, reply to this email right away.";
        emailOutboxService.enqueue(email, "Your SEOPulse account was deleted", intro,
                EmailTemplates.html("Account deleted", intro, null, null));
        log.info("Account deleted: userId={}", userId);
    }

    private void requireSuccessorOwners(Long userId, List<OrganizationMember> memberships) {
        List<String> blocking = memberships.stream()
                .filter(member -> member.getRole() == OrganizationRole.OWNER)
                .filter(member -> {
                    Long organizationId = member.getOrganization().getId();
                    return memberRepository.countByOrganizationId(organizationId) > 1
                            && memberRepository.countByOrganizationIdAndRole(organizationId, OrganizationRole.OWNER) <= 1;
                })
                .map(member -> member.getOrganization().getName())
                .toList();
        if (!blocking.isEmpty()) {
            throw new InvalidStateException("Make another member an owner of " + String.join(", ", blocking)
                    + " before deleting your account.");
        }
    }

    private AccountExport.Workspace workspace(OrganizationMember member) {
        Long organizationId = member.getOrganization().getId();
        List<AccountExport.Project> projects = projectRepository.findByOrganizationIdOrderByIdAsc(organizationId).stream()
                .map(this::project)
                .toList();
        return new AccountExport.Workspace(
                organizationId,
                member.getOrganization().getName(),
                member.getRole().name(),
                entitlementService.planCode(organizationId),
                member.getCreatedAt(),
                projects
        );
    }

    private AccountExport.Project project(Project project) {
        List<AccountExport.Website> websites = websiteRepository.findByProjectIdOrderByIdAsc(project.getId()).stream()
                .map(this::website)
                .toList();
        return new AccountExport.Project(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                websites
        );
    }

    private AccountExport.Website website(Website website) {
        List<AccountExport.Audit> audits = auditRepository.findByWebsiteIdOrderByCreatedAtAsc(website.getId()).stream()
                .map(AccountService::audit)
                .toList();
        return new AccountExport.Website(
                website.getId(),
                website.getName(),
                website.getUrl(),
                website.getStatus() == null ? null : website.getStatus().name(),
                website.getCreatedAt(),
                audits
        );
    }

    private static AccountExport.Audit audit(Audit audit) {
        return new AccountExport.Audit(
                audit.getId(),
                audit.getStatus() == null ? null : audit.getStatus().name(),
                audit.getTriggeredBy() == null ? null : audit.getTriggeredBy().name(),
                audit.getScore(),
                audit.getCategoryScores(),
                audit.getPagesCrawled(),
                audit.getIssueCount(),
                audit.getErrorCount(),
                audit.getWarningCount(),
                audit.getInfoCount(),
                audit.getCreatedAt(),
                audit.getStartedAt(),
                audit.getCompletedAt()
        );
    }

    private User user(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
