package com.seopulse.auth.service;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.auth.entity.PasswordResetToken;
import com.seopulse.auth.repository.PasswordResetTokenRepository;
import com.seopulse.common.exception.InvalidTokenException;
import com.seopulse.user.entity.User;
import com.seopulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final RefreshTokenService refreshTokenService;
    private final AuthProperties properties;
    private final AuthEmails authEmails;

    /**
     * Silent for unknown addresses so the endpoint cannot be used to
     * discover which emails have accounts.
     */
    @Transactional
    public void requestReset(String email) {

        String normalized = email.trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmail(normalized).ifPresentOrElse(user -> {
            Instant now = Instant.now();
            tokenRepository.invalidateAllForUser(user.getId(), now);

            String raw = SecureTokens.generate();
            tokenRepository.save(PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(SecureTokens.sha256Hex(raw))
                    .expiresAt(now.plus(properties.getPasswordResetTtl()))
                    .build());

            authEmails.sendPasswordReset(user, raw);
            log.info("Password reset requested: userId={}", user.getId());
        }, () -> log.info("Password reset requested for unknown email"));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {

        Instant now = Instant.now();

        PasswordResetToken token = tokenRepository.findByTokenHash(SecureTokens.sha256Hex(rawToken))
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new InvalidTokenException("This reset link is invalid or has expired"));

        User user = token.getUser();
        passwordPolicy.validate(newPassword, user.getEmail());

        tokenRepository.invalidateAllForUser(user.getId(), now);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        if (user.getEmailVerifiedAt() == null) {
            // Following the emailed link proves ownership of the address.
            user.setEmailVerifiedAt(now);
        }
        userRepository.save(user);

        refreshTokenService.revokeAll(user.getId());

        log.info("Password reset completed; all sessions revoked: userId={}", user.getId());
    }
}
