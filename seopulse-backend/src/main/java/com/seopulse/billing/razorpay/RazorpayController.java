package com.seopulse.billing.razorpay;

import com.seopulse.common.security.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RazorpayController {

    private final RazorpayBillingService razorpayBillingService;
    private final CurrentUserService currentUserService;

    @PostMapping("/api/v1/orgs/{orgId}/billing/razorpay/subscription")
    public RazorpayBillingService.CheckoutSession subscribe(
            @PathVariable Long orgId,
            @Valid @RequestBody SubscribeRequest request,
            Authentication authentication
    ) {
        return razorpayBillingService.startCheckout(
                orgId, currentUserService.getUserId(authentication), request.plan(), request.interval());
    }

    @PostMapping("/api/v1/orgs/{orgId}/billing/razorpay/verify")
    public ResponseEntity<Void> verify(
            @PathVariable Long orgId,
            @Valid @RequestBody VerifyRequest request,
            Authentication authentication
    ) {
        razorpayBillingService.confirmPayment(orgId, currentUserService.getUserId(authentication),
                request.paymentId(), request.subscriptionId(), request.signature());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/orgs/{orgId}/billing/razorpay/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long orgId, Authentication authentication) {
        razorpayBillingService.cancel(orgId, currentUserService.getUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/billing/razorpay/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId
    ) {
        razorpayBillingService.handleWebhook(payload, signature, eventId);
        return ResponseEntity.ok().build();
    }

    public record SubscribeRequest(@NotBlank String plan, @NotBlank String interval) {
    }

    public record VerifyRequest(
            @NotBlank @Size(max = 100) String paymentId,
            @NotBlank @Size(max = 100) String subscriptionId,
            @NotBlank @Size(max = 200) String signature
    ) {
    }
}
