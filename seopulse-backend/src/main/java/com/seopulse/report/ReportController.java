package com.seopulse.report;

import com.seopulse.common.security.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/audits/{auditId}")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserService currentUserService;

    /** Queues a server-side PDF. Poll {@code GET .../reports} until it is READY. */
    @PostMapping("/reports")
    public ResponseEntity<ReportResponse> request(
            @PathVariable Long projectId,
            @PathVariable Long auditId,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(reportService.request(projectId, auditId, currentUserService.getUserId(authentication)));
    }

    @GetMapping("/reports")
    public List<ReportResponse> list(@PathVariable Long projectId, @PathVariable Long auditId, Authentication authentication) {
        return reportService.list(projectId, auditId, currentUserService.getUserId(authentication));
    }

    /** A signed download URL that expires after a few minutes. */
    @PostMapping("/reports/{reportId}/download-url")
    public ReportUrlSigner.SignedUrl downloadUrl(
            @PathVariable Long projectId,
            @PathVariable Long auditId,
            @PathVariable Long reportId,
            Authentication authentication
    ) {
        return reportService.downloadUrl(projectId, auditId, reportId, currentUserService.getUserId(authentication));
    }

    public record CreateShareRequest(@Min(1) @Max(365) Integer expiresInDays) {
    }

    @PostMapping("/shares")
    public ResponseEntity<ShareResponse> createShare(
            @PathVariable Long projectId,
            @PathVariable Long auditId,
            @RequestBody(required = false) @jakarta.validation.Valid CreateShareRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createShare(
                projectId, auditId, currentUserService.getUserId(authentication),
                request == null ? null : request.expiresInDays()));
    }

    @GetMapping("/shares")
    public List<ShareResponse> listShares(@PathVariable Long projectId, @PathVariable Long auditId, Authentication authentication) {
        return reportService.listShares(projectId, auditId, currentUserService.getUserId(authentication));
    }

    @DeleteMapping("/shares/{shareId}")
    public ResponseEntity<Void> revokeShare(
            @PathVariable Long projectId,
            @PathVariable Long auditId,
            @PathVariable Long shareId,
            Authentication authentication
    ) {
        reportService.revokeShare(projectId, auditId, shareId, currentUserService.getUserId(authentication));
        return ResponseEntity.noContent().build();
    }
}
