package com.seopulse.website.comparison;

import com.seopulse.common.security.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuditInsightsController {

    private final AuditComparisonService comparisonService;
    private final CurrentUserService currentUserService;

    /**
     * New, fixed and persisting issues compared with a baseline audit of the
     * same website (default: the previous completed audit).
     *
     * GET /api/v1/projects/{projectId}/audits/{auditId}/compare?baseline={auditId}
     */
    @GetMapping("/audits/{auditId}/compare")
    public ComparisonResponse compare(
            @PathVariable Long projectId,
            @PathVariable Long auditId,
            @RequestParam(required = false) Long baseline,
            Authentication authentication
    ) {
        return comparisonService.compare(projectId, auditId, baseline, currentUserService.getUserId(authentication));
    }

    /**
     * Score and issue counts of the website's completed audits, oldest first.
     *
     * GET /api/v1/projects/{projectId}/websites/{websiteId}/trend?limit=30
     */
    @GetMapping("/websites/{websiteId}/trend")
    public List<TrendPoint> trend(
            @PathVariable Long projectId,
            @PathVariable Long websiteId,
            @RequestParam(defaultValue = "30") int limit,
            Authentication authentication
    ) {
        return comparisonService.trend(projectId, websiteId, limit, currentUserService.getUserId(authentication));
    }
}
