package com.seopulse.website.service;

import com.seopulse.common.dto.PageResponse;
import com.seopulse.common.exception.DuplicateResourceException;
import com.seopulse.project.entity.Project;
import com.seopulse.project.service.ProjectAccessService;
import com.seopulse.website.dto.CreateWebsiteRequest;
import com.seopulse.website.dto.WebsiteResponse;
import com.seopulse.website.entity.Website;
import com.seopulse.website.entity.WebsiteStatus;
import com.seopulse.website.repository.WebsiteRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

@Service
@RequiredArgsConstructor
@Transactional
public class WebsiteService {

    private final WebsiteRepository websiteRepository;
    private final ProjectAccessService projectAccessService;
    private final UrlValidator urlValidator;

    public WebsiteResponse createWebsite(
            Long projectId,
            Long userId,
            CreateWebsiteRequest request
    ) {

        Project project =
                projectAccessService.requireOwnedProject(
                        projectId,
                        userId
                );

        URI validatedUrl =
                urlValidator.validate(request.url());

        String url = validatedUrl.toString();

        if (websiteRepository
                .existsByProjectIdAndUrl(projectId, url)) {

            throw new DuplicateResourceException(
                    "Website URL already exists in this project"
            );
        }

        Website website = Website.builder()
                .name(request.name().trim())
                .url(url)
                .status(WebsiteStatus.ACTIVE)
                .project(project)
                .build();

        Website savedWebsite =
                websiteRepository.save(website);

        return mapToResponse(savedWebsite);
    }

    @Transactional(readOnly = true)
    public PageResponse<WebsiteResponse> getWebsites(
            Long projectId,
            Long userId,
            Pageable pageable
    ) {

        projectAccessService.requireOwnedProject(
                projectId,
                userId
        );

        Page<WebsiteResponse> page =
                websiteRepository
                        .findByProjectId(projectId, pageable)
                        .map(this::mapToResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public WebsiteResponse getWebsite(
            Long projectId,
            Long websiteId,
            Long userId
    ) {

        Website website =
                projectAccessService.requireOwnedWebsite(
                        projectId,
                        websiteId,
                        userId
                );

        return mapToResponse(website);
    }

    private WebsiteResponse mapToResponse(
            Website website
    ) {

        return new WebsiteResponse(
                website.getId(),
                website.getName(),
                website.getUrl(),
                website.getStatus(),
                website.getProject().getId(),
                website.getCreatedAt(),
                website.getUpdatedAt()
        );
    }
}
