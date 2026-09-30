package com.seopulse.billing;

import com.seopulse.common.security.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final CurrentUserService currentUserService;

    @GetMapping("/api/v1/orgs/{orgId}/billing")
    public BillingService.BillingSnapshot billing(@PathVariable Long orgId, Authentication authentication) {
        return billingService.snapshot(orgId, currentUserService.getUserId(authentication));
    }

    @PostMapping("/api/v1/orgs/{orgId}/billing/checkout")
    public Map<String, String> checkout(
            @PathVariable Long orgId,
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication
    ) {
        String url = billingService.checkout(
                orgId,
                currentUserService.getUserId(authentication),
                request.plan(),
                request.interval()
        );
        return Map.of("url", url);
    }

    @PostMapping("/api/v1/orgs/{orgId}/billing/portal")
    public Map<String, String> portal(@PathVariable Long orgId, Authentication authentication) {
        return Map.of("url", billingService.portal(orgId, currentUserService.getUserId(authentication)));
    }

    @PostMapping("/api/v1/billing/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature
    ) {
        billingService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    public record CheckoutRequest(@NotBlank String plan, @NotBlank String interval) {
    }
}
