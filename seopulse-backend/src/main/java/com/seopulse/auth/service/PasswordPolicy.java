package com.seopulse.auth.service;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.common.exception.WeakPasswordException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class PasswordPolicy {

    /** BCrypt ignores everything after 72 bytes. */
    private static final int MAX_BYTES = 72;

    private final AuthProperties properties;
    private final BreachedPasswordChecker breachedPasswordChecker;

    public void validate(String password, String email) {

        if (password == null || password.length() < properties.getPasswordMinLength()) {
            throw new WeakPasswordException(
                    "Password must be at least " + properties.getPasswordMinLength() + " characters"
            );
        }

        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new WeakPasswordException("Password must be at most " + MAX_BYTES + " bytes");
        }

        if (password.isBlank()) {
            throw new WeakPasswordException("Password must not be blank");
        }

        if (email != null && password.toLowerCase(Locale.ROOT).contains(localPart(email))) {
            throw new WeakPasswordException("Password must not contain your email address");
        }

        if (breachedPasswordChecker.isBreached(password)) {
            throw new WeakPasswordException(
                    "This password has appeared in a data breach. Choose a different one."
            );
        }
    }

    private static String localPart(String email) {
        String lower = email.toLowerCase(Locale.ROOT);
        int at = lower.indexOf('@');
        String local = at > 0 ? lower.substring(0, at) : lower;
        return local.length() >= 4 ? local : "\u0000";
    }
}
