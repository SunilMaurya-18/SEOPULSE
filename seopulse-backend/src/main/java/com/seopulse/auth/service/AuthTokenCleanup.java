package com.seopulse.auth.service;

import com.seopulse.auth.repository.EmailVerificationTokenRepository;
import com.seopulse.auth.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthTokenCleanup {

    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Transactional
    public void deleteExpired() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(1));
        int refresh = refreshTokenService.deleteExpired();
        int verification = emailVerificationTokenRepository.deleteExpiredBefore(cutoff);
        int reset = passwordResetTokenRepository.deleteExpiredBefore(cutoff);
        if (refresh + verification + reset > 0) {
            log.info("Deleted expired tokens: refresh={}, verification={}, reset={}", refresh, verification, reset);
        }
    }
}
