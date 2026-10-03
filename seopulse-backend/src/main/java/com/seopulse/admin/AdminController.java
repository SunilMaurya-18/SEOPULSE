package com.seopulse.admin;

import com.seopulse.common.dto.PageResponse;
import com.seopulse.common.security.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;
    private final CurrentUserService currentUserService;

    @GetMapping("/stats")
    public AdminDtos.Stats stats(Authentication authentication) {
        requireAdmin(authentication);
        return adminService.stats();
    }

    @GetMapping("/users")
    public PageResponse<AdminDtos.UserRow> users(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return adminService.users(q, page, size);
    }

    @PostMapping("/users/{userId}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable Long userId, Authentication authentication) {
        adminService.unlockUser(userId, requireAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/organizations")
    public PageResponse<AdminDtos.OrganizationRow> organizations(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return adminService.organizations(q, page, size);
    }

    @GetMapping("/audits/failed")
    public PageResponse<AdminDtos.FailedAuditRow> failedAudits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return adminService.failedAudits(page, size);
    }

    private Long requireAdmin(Authentication authentication) {
        Long userId = currentUserService.getUserId(authentication);
        adminService.requireAdmin(userId);
        return userId;
    }
}
