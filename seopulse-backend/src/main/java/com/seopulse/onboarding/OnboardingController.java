package com.seopulse.onboarding;

import com.seopulse.common.security.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final CurrentUserService currentUserService;

    @GetMapping("/projects/{projectId}/onboarding")
    public OnboardingResponse status(@PathVariable Long projectId, Authentication authentication) {
        return onboardingService.status(projectId, currentUserService.getUserId(authentication));
    }

    @PostMapping("/onboarding/dismiss")
    public ResponseEntity<Void> dismiss(Authentication authentication) {
        onboardingService.dismiss(currentUserService.getUserId(authentication));
        return ResponseEntity.noContent().build();
    }
}
