package com.seopulse.auth.service;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.auth.entity.RefreshToken;
import com.seopulse.auth.repository.RefreshTokenRepository;
import com.seopulse.common.exception.InvalidCredentialsException;
import com.seopulse.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Rotating refresh tokens. Each refresh consumes the presented token and
 * issues a new one in the same family. Presenting an already-consumed token
 * means it was stolen (or replayed), so the whole family is revoked.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final AuthProperties properties;
    private final TransactionTemplate transactionTemplate;

    public record IssuedToken(String rawToken, User user, Duration ttl) {
    }

    @Transactional
    public IssuedToken issue(User user) {
        return create(user, UUID.randomUUID());
    }

    /**
     * @throws InvalidCredentialsException if the token is unknown, expired,
     *                                     revoked or already used
     */
    public IssuedToken rotate(String rawToken) {

        Instant now = Instant.now();
        String hash = SecureTokens.sha256Hex(rawToken);

        RotationOutcome outcome = transactionTemplate.execute(status -> {

            RefreshToken token = repository.findByTokenHashForUpdate(hash).orElse(null);

            if (token == null || token.getRevokedAt() != null || token.getExpiresAt().isBefore(now)) {
                return RotationOutcome.rejected();
            }

            if (token.getUsedAt() != null) {
                return RotationOutcome.reused(token.getFamilyId(), token.getUser().getId());
            }

            token.setUsedAt(now);
            return RotationOutcome.rotated(create(token.getUser(), token.getFamilyId()));
        });

        if (outcome.reusedFamily() != null) {
            // Committed separately: the revocation must survive the rejection below.
            transactionTemplate.executeWithoutResult(status ->
                    repository.revokeFamily(outcome.reusedFamily(), now));
            log.warn("Refresh token reuse detected; session family revoked: userId={}, familyId={}",
                    outcome.userId(), outcome.reusedFamily());
            throw new InvalidCredentialsException("Session expired. Please sign in again.");
        }

        if (outcome.issued() == null) {
            throw new InvalidCredentialsException("Session expired. Please sign in again.");
        }

        return outcome.issued();
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(SecureTokens.sha256Hex(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId(), Instant.now()));
    }

    @Transactional
    public void revokeAll(Long userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    @Transactional
    public int deleteExpired() {
        return repository.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(1)));
    }

    private IssuedToken create(User user, UUID familyId) {

        String raw = SecureTokens.generate();

        repository.save(RefreshToken.builder()
                .user(user)
                .familyId(familyId)
                .tokenHash(SecureTokens.sha256Hex(raw))
                .expiresAt(Instant.now().plus(properties.getRefreshTokenTtl()))
                .build());

        return new IssuedToken(raw, user, properties.getRefreshTokenTtl());
    }

    private record RotationOutcome(IssuedToken issued, UUID reusedFamily, Long userId) {

        static RotationOutcome rotated(IssuedToken issued) {
            return new RotationOutcome(issued, null, null);
        }

        static RotationOutcome reused(UUID familyId, Long userId) {
            return new RotationOutcome(null, familyId, userId);
        }

        static RotationOutcome rejected() {
            return new RotationOutcome(null, null, null);
        }
    }
}
