package com.seopulse.onboarding;

import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.onboarding.OnboardingResponse.Step;
import com.seopulse.onboarding.OnboardingResponse.StepId;
import com.seopulse.organization.repository.InvitationRepository;
import com.seopulse.organization.repository.OrganizationMemberRepository;
import com.seopulse.project.entity.Project;
import com.seopulse.project.service.ProjectAccessService;
import com.seopulse.report.ReportRepository;
import com.seopulse.report.ReportShareRepository;
import com.seopulse.user.entity.User;
import com.seopulse.user.repository.UserRepository;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.repository.WebsiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Getting-started progress, derived from what already exists in the workspace. */
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final ProjectAccessService projectAccessService;
    private final UserRepository userRepository;
    private final WebsiteRepository websiteRepository;
    private final AuditRepository auditRepository;
    private final ReportRepository reportRepository;
    private final ReportShareRepository reportShareRepository;
    private final OrganizationMemberRepository memberRepository;
    private final InvitationRepository invitationRepository;

    @Transactional(readOnly = true)
    public OnboardingResponse status(Long projectId, Long userId) {
        Project project = projectAccessService.requireOwnedProject(projectId, userId);
        User user = requireUser(userId);
        Long organizationId = project.getOrganization().getId();

        boolean reportShared = reportShareRepository.existsForProject(projectId)
                || reportRepository.existsForProject(projectId);
        boolean teamInvited = memberRepository.countByOrganizationId(organizationId) > 1
                || invitationRepository.countByOrganizationIdAndAcceptedAtIsNullAndRevokedAtIsNull(organizationId) > 0;

        return new OnboardingResponse(user.getOnboardingDismissedAt() != null, List.of(
                new Step(StepId.VERIFY_EMAIL, user.isEmailVerified()),
                new Step(StepId.ADD_WEBSITE, websiteRepository.countByProjectId(projectId) > 0),
                new Step(StepId.RUN_AUDIT,
                        auditRepository.countByWebsiteProjectIdAndStatus(projectId, AuditStatus.COMPLETED) > 0),
                new Step(StepId.SHARE_REPORT, reportShared),
                new Step(StepId.INVITE_TEAM, teamInvited)
        ));
    }

    @Transactional
    public void dismiss(Long userId) {
        User user = requireUser(userId);
        if (user.getOnboardingDismissedAt() == null) {
            user.setOnboardingDismissedAt(Instant.now());
        }
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
