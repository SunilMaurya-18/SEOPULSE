package com.seopulse.auth.service;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.common.email.EmailMessage;
import com.seopulse.common.email.EmailSender;
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

    private final EmailSender emailSender;
    private final AuthProperties properties;

    public void sendVerification(User user, String rawToken) {
        send(new EmailMessage(
                user.getEmail(),
                "Verify your SEOPulse email address",
                """
                Hi %s,

                Confirm your email address to start running audits:
                %s

                The link expires in %d hours. If you did not create an account, ignore this email.
                """.formatted(
                        user.getName(),
                        link("/verify-email", rawToken),
                        properties.getEmailVerificationTtl().toHours()
                )
        ));
    }

    public void sendPasswordReset(User user, String rawToken) {
        send(new EmailMessage(
                user.getEmail(),
                "Reset your SEOPulse password",
                """
                Hi %s,

                Reset your password with this link:
                %s

                The link expires in %d minutes and can be used once. If you did not ask for this, ignore this email.
                """.formatted(
                        user.getName(),
                        link("/reset-password", rawToken),
                        properties.getPasswordResetTtl().toMinutes()
                )
        ));
    }

    public void sendAccountLocked(User user) {
        send(new EmailMessage(
                user.getEmail(),
                "SEOPulse sign-in temporarily locked",
                """
                Hi %s,

                We locked sign-in to your account for %d minutes after %d failed attempts.
                If this wasn't you, reset your password: %s
                """.formatted(
                        user.getName(),
                        properties.getLockoutDuration().toMinutes(),
                        properties.getMaxFailedLogins(),
                        properties.getAppBaseUrl() + "/forgot-password"
                )
        ));
    }

    private String link(String path, String rawToken) {
        return properties.getAppBaseUrl() + path + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private void send(EmailMessage message) {
        try {
            emailSender.send(message);
        } catch (RuntimeException ex) {
            log.error("Failed to send email: subject={}", message.subject(), ex);
        }
    }
}
