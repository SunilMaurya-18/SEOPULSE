package com.seopulse.project.service;

import com.seopulse.common.dto.PageResponse;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.project.dto.CreateProjectRequest;
import com.seopulse.project.dto.ProjectResponse;
import com.seopulse.project.dto.ProjectSummaryResponse;
import com.seopulse.project.entity.Project;
import com.seopulse.project.repository.ProjectRepository;
import com.seopulse.user.entity.User;
import com.seopulse.user.repository.UserRepository;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.repository.AuditPageRepository;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.repository.WebsiteRepository;
import com.seopulse.website.seo.repository.SeoIssueRepository;
import com.seopulse.website.repository.AuditOutboxRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;
    private final UserRepository userRepository;

    private final WebsiteRepository websiteRepository;
    private final AuditRepository auditRepository;
    private final AuditPageRepository auditPageRepository;
    private final AuditOutboxRepository auditOutboxRepository;
    private final SeoIssueRepository seoIssueRepository;

    public ProjectResponse createProject(
            CreateProjectRequest request,
            Long userId
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        Project project = Project.builder()
                .name(request.name().trim())
                .description(
                        request.description() == null
                                ? null
                                : request.description().trim()
                )
                .user(user)
                .build();

        Project savedProject =
                projectRepository.save(project);

        return mapToResponse(savedProject);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getProjects(
            Long userId,
            Pageable pageable
    ) {

        Page<ProjectResponse> page =
                projectRepository
                        .findByUserId(userId, pageable)
                        .map(this::mapToResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(
            Long projectId,
            Long userId
    ) {

        Project project =
                projectAccessService.requireOwnedProject(
                        projectId,
                        userId
                );

        return mapToResponse(project);
    }

    /**
     * Deletes a project and all of its dependent data.
     *
     * FK deletion order:
     *
     * SeoIssue
     *     ↓
     * AuditPage
     *     ↓
     * AuditOutbox
     *     ↓
     * Audit
     *     ↓
     * Website
     *     ↓
     * Project
     */
    public void deleteProject(
            Long projectId,
            Long userId
    ) {

        Project project =
                projectAccessService.requireOwnedProject(
                        projectId,
                        userId
                );

        List<Long> websiteIds =
                websiteRepository
                        .findIdsByProjectId(projectId);

        for (Long websiteId : websiteIds) {

            List<Long> auditIds =
                    auditRepository
                            .findIdsByWebsiteId(websiteId);

            for (Long auditId : auditIds) {

                // 1. SEO issues
                seoIssueRepository.deleteByAuditId(auditId);

                // 2. Audit pages
                auditPageRepository.deleteByAuditId(auditId);

                // 3. Outbox events
                auditOutboxRepository.deleteByAuditId(auditId);
            }

            // 4. Audits
            auditRepository.deleteByWebsiteId(websiteId);
        }

        // 5. Websites
        websiteRepository.deleteByProjectId(projectId);

        // 6. Project
        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public ProjectSummaryResponse getProjectSummary(
            Long projectId,
            Long userId
    ) {

        Project project =
                projectAccessService.requireOwnedProject(
                        projectId,
                        userId
                );

        long totalWebsites =
                websiteRepository
                        .countByProjectId(projectId);

        long totalAudits =
                auditRepository
                        .countByWebsiteProjectId(projectId);

        long completedAudits =
                auditRepository
                        .countByWebsiteProjectIdAndStatus(
                                projectId,
                                AuditStatus.COMPLETED
                        );

        long failedAudits =
                auditRepository
                        .countByWebsiteProjectIdAndStatus(
                                projectId,
                                AuditStatus.FAILED
                        );

        long activeAudits =
                auditRepository
                        .countByWebsiteProjectIdAndStatusIn(
                                projectId,
                                List.of(
                                        AuditStatus.QUEUED,
                                        AuditStatus.CRAWLING,
                                        AuditStatus.ANALYZING
                                )
                        );

        return new ProjectSummaryResponse(
                project.getId(),
                project.getName(),
                totalWebsites,
                totalAudits,
                completedAudits,
                failedAudits,
                activeAudits
        );
    }

    private ProjectResponse mapToResponse(
            Project project
    ) {

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}