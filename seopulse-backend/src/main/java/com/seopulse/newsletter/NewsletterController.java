package com.seopulse.newsletter;

import com.seopulse.auth.service.SecureTokens;
import com.seopulse.common.ratelimit.ClientIp;
import com.seopulse.common.ratelimit.RateLimitProperties;
import com.seopulse.common.ratelimit.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    /** Always 202, whether the address is new, pending or already subscribed. */
    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(
            @Valid @RequestBody SubscribeRequest request,
            HttpServletRequest servletRequest
    ) {
        rateLimiter.enforce("newsletter-ip", ClientIp.of(servletRequest), rateLimitProperties.getNewsletter());
        String emailKey = SecureTokens.sha256Hex(NewsletterService.normalize(request.email())).substring(0, 32);
        if (rateLimiter.tryConsume("newsletter-email", emailKey, rateLimitProperties.getNewsletter()).allowed()) {
            newsletterService.subscribe(request.email());
        }
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody TokenRequest request) {
        newsletterService.confirm(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@Valid @RequestBody TokenRequest request) {
        newsletterService.unsubscribe(request.token());
        return ResponseEntity.noContent().build();
    }

    public record SubscribeRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @AssertTrue(message = "Consent is required to subscribe") boolean consent
    ) {
    }

    public record TokenRequest(@NotBlank @Size(max = 64) String token) {
    }
}
