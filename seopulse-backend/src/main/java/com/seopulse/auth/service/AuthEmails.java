package com.seopulse.auth.service;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.notification.EmailOutboxService;
import com.seopulse.notification.EmailTemplates;
import com.seopulse.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEmails {

    private final EmailOutboxService emailOutboxService;
    private final AuthProperties properties;

    public void sendVerification(User user, String rawToken) {
        String url = link("/verify-email", rawToken);
        String text = "Hi %s, confirm your email to start audits: %s".formatted(user.getName(), url);
        enqueue(user.getEmail(), "Verify your SEOPulse email address", text,
                EmailTemplates.html("Verify your email", text, "Verify email", url));
    }

    public void sendPasswordReset(User user, String rawToken) {
        String url = link("/reset-password", rawToken);
        String text = "Hi %s, reset your password: %s".formatted(user.getName(), url);
        enqueue(user.getEmail(), "Reset your SEOPulse password", text,
                EmailTemplates.html("Reset your password", text, "Reset password", url));
    }

    public void sendAccountLocked(User user) {
        String url = properties.getAppBaseUrl() + "/forgot-password";
        String text = "Hi %s, sign-in is locked for %d minutes. Reset your password: %s".formatted(
                user.getName(),
                properties.getLockoutDuration().toMinutes(),
                url
        );
        enqueue(user.getEmail(), "SEOPulse sign-in temporarily locked", text,
                EmailTemplates.html("Sign-in locked", text, "Reset password", url));
    }

    private String link(String path, String rawToken) {
        return properties.getAppBaseUrl() + path + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private void enqueue(String to, String subject, String text, String html) {
        try {
            emailOutboxService.enqueue(to, subject, text, html);
        } catch (RuntimeException ex) {
            log.error("Failed to queue email: subject={}", subject, ex);
        }
    }
}
