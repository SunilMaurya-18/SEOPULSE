package com.seopulse.project.service;

import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.project.entity.Project;
import com.seopulse.project.repository.ProjectRepository;
import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.Website;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.repository.WebsiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single entry point for resource ownership checks.
 * <p>
 * Resources that exist but belong to another user are reported
 * as not found, so IDs cannot be discovered by probing.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final WebsiteRepository websiteRepository;
    private final AuditRepository auditRepository;

    public Project requireOwnedProject(
            Long projectId,
            Long userId
    ) {

        return projectRepository
                .findAccessible(projectId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found"
                        )
                );
    }

    public Website requireOwnedWebsite(
            Long projectId,
            Long websiteId,
            Long userId
    ) {

        return websiteRepository
                .findByIdAndProjectIdAndProjectUserId(
                        websiteId,
                        projectId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Website not found"
                        )
                );
    }

    public Audit requireOwnedAudit(
            Long projectId,
            Long auditId,
            Long userId
    ) {

        return auditRepository
                .findByIdAndWebsiteProjectIdAndWebsiteProjectUserId(
                        auditId,
                        projectId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Audit not found"
                        )
                );
    }
}
