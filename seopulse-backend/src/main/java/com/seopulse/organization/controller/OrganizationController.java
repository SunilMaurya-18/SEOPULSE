package com.seopulse.organization.controller;

import com.seopulse.common.security.CurrentUserService;
import com.seopulse.organization.entity.Invitation;
import com.seopulse.organization.entity.OrganizationMember;
import com.seopulse.organization.entity.OrganizationRole;
import com.seopulse.organization.service.OrganizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orgs")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<OrgResponse> list(Authentication authentication) {
        Long userId = currentUserService.getUserId(authentication);
        return organizationService.listForUser(userId).stream().map(OrgResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrgResponse create(@Valid @RequestBody NameRequest request, Authentication authentication) {
        var organization = organizationService.create(currentUserService.getUserId(authentication), request.name());
        return new OrgResponse(organization.getId(), organization.getName(), organization.getSlug(), "OWNER");
    }

    @PatchMapping("/{orgId}")
    public OrgResponse rename(
            @PathVariable Long orgId,
            @Valid @RequestBody NameRequest request,
            Authentication authentication
    ) {
        var organization = organizationService.rename(orgId, currentUserService.getUserId(authentication), request.name());
        return new OrgResponse(organization.getId(), organization.getName(), organization.getSlug(), null);
    }

    @DeleteMapping("/{orgId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long orgId, Authentication authentication) {
        organizationService.delete(orgId, currentUserService.getUserId(authentication));
    }

    @GetMapping("/{orgId}/members")
    public List<MemberResponse> members(@PathVariable Long orgId, Authentication authentication) {
        return organizationService.members(orgId, currentUserService.getUserId(authentication)).stream()
                .map(MemberResponse::from)
                .toList();
    }

    @GetMapping("/{orgId}/invitations")
    public List<InviteResponse> invitations(@PathVariable Long orgId, Authentication authentication) {
        return organizationService.invitations(orgId, currentUserService.getUserId(authentication)).stream()
                .map(InviteResponse::from)
                .toList();
    }

    @PostMapping("/{orgId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse invite(
            @PathVariable Long orgId,
            @Valid @RequestBody InviteRequest request,
            Authentication authentication
    ) {
        Invitation invitation = organizationService.invite(
                orgId,
                currentUserService.getUserId(authentication),
                request.email(),
                request.role()
        );
        return InviteResponse.from(invitation);
    }

    @DeleteMapping("/{orgId}/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(
            @PathVariable Long orgId,
            @PathVariable Long invitationId,
            Authentication authentication
    ) {
        organizationService.revokeInvite(orgId, currentUserService.getUserId(authentication), invitationId);
    }

    @PostMapping("/invitations/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@Valid @RequestBody AcceptRequest request, Authentication authentication) {
        organizationService.accept(currentUserService.getUserId(authentication), request.token());
    }

    @PatchMapping("/{orgId}/members/{memberUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeRole(
            @PathVariable Long orgId,
            @PathVariable Long memberUserId,
            @Valid @RequestBody RoleRequest request,
            Authentication authentication
    ) {
        organizationService.changeRole(orgId, currentUserService.getUserId(authentication), memberUserId, request.role());
    }

    @DeleteMapping("/{orgId}/members/{memberUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable Long orgId,
            @PathVariable Long memberUserId,
            Authentication authentication
    ) {
        organizationService.removeMember(orgId, currentUserService.getUserId(authentication), memberUserId);
    }

    @PostMapping("/{orgId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long orgId, Authentication authentication) {
        organizationService.leave(orgId, currentUserService.getUserId(authentication));
    }

    public record NameRequest(@NotBlank String name) {
    }

    public record InviteRequest(@NotBlank @Email String email, @NotNull OrganizationRole role) {
    }

    public record AcceptRequest(@NotBlank String token) {
    }

    public record RoleRequest(@NotNull OrganizationRole role) {
    }

    public record OrgResponse(Long id, String name, String slug, String role) {
        static OrgResponse from(OrganizationMember member) {
            return new OrgResponse(
                    member.getOrganization().getId(),
                    member.getOrganization().getName(),
                    member.getOrganization().getSlug(),
                    member.getRole().name()
            );
        }
    }

    public record MemberResponse(Long userId, String name, String email, String role) {
        static MemberResponse from(OrganizationMember member) {
            return new MemberResponse(
                    member.getUser().getId(),
                    member.getUser().getName(),
                    member.getUser().getEmail(),
                    member.getRole().name()
            );
        }
    }

    public record InviteResponse(Long id, String email, String role) {
        static InviteResponse from(Invitation invitation) {
            return new InviteResponse(invitation.getId(), invitation.getEmail(), invitation.getRole().name());
        }
    }
}
