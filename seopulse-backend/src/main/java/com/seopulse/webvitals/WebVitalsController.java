package com.seopulse.webvitals;

import com.seopulse.common.security.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/audits/{auditId}/web-vitals")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WebVitalsController {

    private final WebVitalsService webVitalsService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<WebVitalsResponse> get(
            @PathVariable Long projectId,
            @PathVariable Long auditId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(webVitalsService.get(projectId, auditId, currentUserService.getUserId(authentication)));
    }
}
