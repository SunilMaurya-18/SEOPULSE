package com.seopulse.website.controller;

import com.seopulse.common.dto.PageResponse;
import com.seopulse.common.security.CurrentUserService;
import com.seopulse.website.dto.AuditPageResponse;
import com.seopulse.website.dto.AuditResponse;
import com.seopulse.website.dto.AuditSummaryResponse;
import com.seopulse.website.dto.SeoIssueResponse;
import com.seopulse.website.service.AuditService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/audits")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    /**
     * Create a new SEO audit.
     *
     * POST /api/v1/projects/{projectId}/audits?websiteId={websiteId}
     */
    @PostMapping
    public ResponseEntity<AuditResponse> createAudit(
            @PathVariable Long projectId,
            @RequestParam Long websiteId,
            Authentication authentication
    ) {
        Long userId = currentUserService.getUserId(authentication);

        AuditResponse response =
                auditService.createAudit(projectId, websiteId, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get audits for a website.
     *
     * GET /api/v1/projects/{projectId}/audits?websiteId={websiteId}
     *     &page=0&size=20
     */
    @GetMapping
    public ResponseEntity<PageResponse<AuditResponse>> getAudits(
            @PathVariable Long projectId,
            @RequestParam Long websiteId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        Long userId = currentUserService.getUserId(authentication);

        PageResponse<AuditResponse> response =
                auditService.getAudits(
                        projectId,
                        websiteId,
                        userId,
                        status,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Get a single audit.
     *
     * GET /api/v1/projects/{projectId}/audits/{auditId}
     */
    @GetMapping("/{auditId}")
    public ResponseEntity<AuditResponse> getAudit(
            @PathVariable Long projectId,
            @PathVariable Long auditId,
            Authentication authentication
    ) {
        Long userId = currentUserService.getUserId(authentication);

        AuditResponse response =
                auditService.getAudit(projectId, auditId, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get audit summary.
     *
     * GET /api/v1/projects/{projectId}/audits/{auditId}/summary
     */
    @GetMapping("/{auditId}/summary")
    public ResponseEntity<AuditSummaryResponse> getAuditSummary(
            @PathVariable Long projectId,
            @PathVariable Long auditId,
            Authentication authentication
    ) {
        Long userId = currentUserService.getUserId(authentication);

        AuditSummaryResponse response =
                auditService.getAuditSummary(projectId, auditId, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get pages discovered/crawled during an audit.
     *
     * GET /api/v1/projects/{projectId}/audits/{auditId}/pages
     */
    @GetMapping("/{auditId}/pages")
    public ResponseEntity<PageResponse<AuditPageResponse>> getAuditPages(
            @PathVariable Long projectId,
            @PathVariable Long auditId,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        Long userId = currentUserService.getUserId(authentication);

        PageResponse<AuditPageResponse> response =
                auditService.getAuditPages(
                        projectId,
                        auditId,
                        userId,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Get SEO issues detected during an audit.
     *
     * GET /api/v1/projects/{projectId}/audits/{auditId}/issues
     */
    @GetMapping("/{auditId}/issues")
    public ResponseEntity<PageResponse<SeoIssueResponse>> getAuditIssues(
            @PathVariable Long projectId,
            @PathVariable Long auditId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String ruleCode,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        Long userId = currentUserService.getUserId(authentication);

        PageResponse<SeoIssueResponse> response =
                auditService.getAuditIssues(
                        projectId,
                        auditId,
                        userId,
                        severity,
                        ruleCode,
                        pageable
                );

        return ResponseEntity.ok(response);
    }
}
