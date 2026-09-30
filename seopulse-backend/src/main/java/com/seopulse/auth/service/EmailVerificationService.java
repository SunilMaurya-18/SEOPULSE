package com.seopulse.auth.service;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.auth.entity.EmailVerificationToken;
import com.seopulse.auth.repository.EmailVerificationTokenRepository;
import com.seopulse.common.exception.InvalidStateException;
import com.seopulse.common.exception.InvalidTokenException;
import com.seopulse.common.exception.ResourceNotFoundException;
import com.seopulse.user.entity.User;
import com.seopulse.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final AuthProperties properties;
    private final AuthEmails authEmails;

    @Transactional
    public void sendVerification(User user) {

        Instant now = Instant.now();
        tokenRepository.invalidateAllForUser(user.getId(), now);

        String raw = SecureTokens.generate();
        tokenRepository.save(EmailVerificationToken.builder()
                .user(user)
                .tokenHash(SecureTokens.sha256Hex(raw))
                .expiresAt(now.plus(properties.getEmailVerificationTtl()))
                .build());

        authEmails.sendVerification(user, raw);
    }

    @Transactional
    public void resend(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            throw new InvalidStateException("Email address is already verified");
        }

        sendVerification(user);
    }

    @Transactional
    public User verify(String rawToken) {

        Instant now = Instant.now();

        EmailVerificationToken token = tokenRepository.findByTokenHash(SecureTokens.sha256Hex(rawToken))
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new InvalidTokenException("This verification link is invalid or has expired"));

        token.setUsedAt(now);

        User user = token.getUser();
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(now);
        }

        log.info("Email verified: userId={}", user.getId());
        return user;
    }
}
