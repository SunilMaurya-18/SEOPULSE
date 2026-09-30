package com.seopulse.auth.service;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.auth.dto.AuthResponse;
import com.seopulse.auth.dto.LoginRequest;
import com.seopulse.auth.dto.RegisterRequest;
import com.seopulse.common.exception.AccountLockedException;
import com.seopulse.common.exception.DuplicateResourceException;
import com.seopulse.common.exception.InvalidCredentialsException;
import com.seopulse.organization.service.OrganizationProvisioningService;
import com.seopulse.user.entity.Role;
import com.seopulse.user.entity.User;
import com.seopulse.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Sign-up, sign-in and session refresh. Methods here are deliberately not
 * transactional: failed-login counters must persist even though the
 * request ends in an exception.
 */
@Service
@Slf4j
public class AuthService {

    public record AuthSession(AuthResponse response, String refreshToken, Duration refreshTtl) {
    }

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final AuthEmails authEmails;
    private final AuthProperties properties;
    private final OrganizationProvisioningService organizationProvisioningService;

    /** Compared against when the email is unknown, so timing does not reveal which accounts exist. */
    private final String dummyHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            EmailVerificationService emailVerificationService,
            AuthEmails authEmails,
            AuthProperties properties,
            OrganizationProvisioningService organizationProvisioningService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailVerificationService = emailVerificationService;
        this.authEmails = authEmails;
        this.properties = properties;
        this.organizationProvisioningService = organizationProvisioningService;
        this.dummyHash = passwordEncoder.encode("seopulse-timing-equalizer");
    }

    public AuthSession register(RegisterRequest request) {

        String email = normalize(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }

        passwordPolicy.validate(request.password(), email);

        User user;
        try {
            user = userRepository.save(User.builder()
                    .name(request.name().trim())
                    .email(email)
                    .password(passwordEncoder.encode(request.password()))
                    .role(Role.USER)
                    .termsAcceptedVersion("2026-09-30")
                    .termsAcceptedAt(Instant.now())
                    .build());
            organizationProvisioningService.ensureFor(user);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Email already exists");
        }

        emailVerificationService.sendVerification(user);

        log.info("User registered: userId={}", user.getId());

        return startSession(user);
    }

    public AuthSession login(LoginRequest request) {

        String email = normalize(request.email());
        Instant now = Instant.now();

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            log.warn("Failed login attempt: unknown account");
            throw invalidCredentials();
        }

        if (user.isLocked(now)) {
            throw new AccountLockedException(secondsUntil(user.getLockedUntil(), now));
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            recordFailedLogin(user, now);
            throw invalidCredentials();
        }

        if (user.getFailedLoginCount() > 0 || user.getLockedUntil() != null) {
            userRepository.resetLoginFailures(user.getId());
        }

        log.info("User logged in: userId={}", user.getId());

        return startSession(user);
    }

    public AuthSession refresh(String rawRefreshToken) {

        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidCredentialsException("Session expired. Please sign in again.");
        }

        RefreshTokenService.IssuedToken issued = refreshTokenService.rotate(rawRefreshToken);
        User user = issued.user();

        return new AuthSession(toResponse(user), issued.rawToken(), issued.ttl());
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    public void logoutEverywhere(Long userId) {
        refreshTokenService.revokeAll(userId);
        log.info("All sessions revoked: userId={}", userId);
    }

    private AuthSession startSession(User user) {
        RefreshTokenService.IssuedToken issued = refreshTokenService.issue(user);
        return new AuthSession(toResponse(user), issued.rawToken(), issued.ttl());
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name()),
                "Bearer",
                jwtService.accessTokenTtlSeconds(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.isEmailVerified(),
                properties.isRequireEmailVerification()
        );
    }

    private void recordFailedLogin(User user, Instant now) {

        userRepository.incrementFailedLogins(user.getId());

        Instant until = now.plus(properties.getLockoutDuration());
        int locked = userRepository.lockIfThresholdReached(user.getId(), properties.getMaxFailedLogins(), until);

        if (locked == 1) {
            log.warn("Account locked after repeated failed logins: userId={}", user.getId());
            authEmails.sendAccountLocked(user);
            throw new AccountLockedException(secondsUntil(until, now));
        }

        log.warn("Failed login attempt: userId={}", user.getId());
    }

    private static long secondsUntil(Instant until, Instant now) {
        return Math.max(1, Duration.between(now, until).toSeconds());
    }

    private static InvalidCredentialsException invalidCredentials() {
        return new InvalidCredentialsException("Invalid email or password");
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
