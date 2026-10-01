package com.seopulse.organization.controller;

import com.seopulse.common.security.CurrentUserService;
import com.seopulse.organization.entity.OrganizationBranding;
import com.seopulse.organization.service.BrandingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** White-label settings for reports. Saving requires the Agency plan. */
@RestController
@RequestMapping("/api/v1/orgs/{orgId}/branding")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BrandingController {

    private final BrandingService brandingService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public BrandingService.BrandingResponse get(@PathVariable Long orgId, Authentication authentication) {
        return brandingService.get(orgId, currentUserService.getUserId(authentication));
    }

    @PutMapping
    public BrandingService.BrandingResponse put(
            @PathVariable Long orgId,
            @RequestBody OrganizationBranding request,
            Authentication authentication
    ) {
        return brandingService.update(orgId, currentUserService.getUserId(authentication), request);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@PathVariable Long orgId, Authentication authentication) {
        brandingService.clear(orgId, currentUserService.getUserId(authentication));
        return ResponseEntity.noContent().build();
    }
}
