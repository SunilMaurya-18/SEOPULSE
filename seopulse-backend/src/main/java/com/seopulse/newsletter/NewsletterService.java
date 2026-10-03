package com.seopulse.newsletter;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.auth.service.SecureTokens;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.notification.EmailOutboxService;
import com.seopulse.notification.EmailTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterSubscriberRepository repository;
    private final EmailOutboxService emailOutboxService;
    private final AuthProperties authProperties;

    /**
     * Records consent and emails a confirmation link. Addresses that are
     * already confirmed get no email, so the endpoint cannot be used to spam
     * existing subscribers.
     */
    @Transactional
    public void subscribe(String rawEmail) {
        String email = normalize(rawEmail);
        Instant now = Instant.now();
        NewsletterSubscriber subscriber = repository.findByEmail(email).orElse(null);

        if (subscriber != null && subscriber.isActive()) {
            return;
        }

        if (subscriber == null) {
            subscriber = repository.save(NewsletterSubscriber.builder()
                    .email(email)
                    .token(SecureTokens.generate())
                    .consentAt(now)
                    .build());
        } else {
            subscriber.setConsentAt(now);
            subscriber.setConfirmedAt(null);
            subscriber.setUnsubscribedAt(null);
        }

        String url = link("/newsletter/confirm", subscriber.getToken());
        String intro = "Confirm that you want SEOPulse product news and SEO tips at this address. "
                + "If you did not ask for this, ignore this email and nothing will be sent.";
        emailOutboxService.enqueue(
                email,
                "Confirm your SEOPulse subscription",
                EmailTemplates.text(intro, url),
                EmailTemplates.html("Confirm your subscription", intro, "Confirm subscription", url)
        );
    }

    @Transactional
    public void confirm(String token) {
        NewsletterSubscriber subscriber = byToken(token);
        if (subscriber.getConfirmedAt() == null) {
            subscriber.setConfirmedAt(Instant.now());
        }
        subscriber.setUnsubscribedAt(null);
    }

    @Transactional
    public void unsubscribe(String token) {
        NewsletterSubscriber subscriber = byToken(token);
        if (subscriber.getUnsubscribedAt() == null) {
            subscriber.setUnsubscribedAt(Instant.now());
        }
    }

    private NewsletterSubscriber byToken(String token) {
        return repository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("This link is invalid or has expired"));
    }

    private String link(String path, String token) {
        return authProperties.getAppBaseUrl() + path + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
