package com.seopulse.auth.controller;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.auth.dto.AuthResponse;
import com.seopulse.auth.dto.ForgotPasswordRequest;
import com.seopulse.auth.dto.LoginRequest;
import com.seopulse.auth.dto.RegisterRequest;
import com.seopulse.auth.dto.ResetPasswordRequest;
import com.seopulse.auth.dto.TokenRequest;
import com.seopulse.auth.service.AuthService;
import com.seopulse.auth.service.EmailVerificationService;
import com.seopulse.auth.service.PasswordResetService;
import com.seopulse.auth.service.SecureTokens;
import com.seopulse.common.exception.InvalidCredentialsException;
import com.seopulse.common.ratelimit.ClientIp;
import com.seopulse.common.ratelimit.RateLimitProperties;
import com.seopulse.common.ratelimit.RateLimiter;
import com.seopulse.common.security.CurrentUserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * Required on cookie-authenticated endpoints. Browsers only send custom
     * headers cross-origin after a CORS preflight, which blocks CSRF even if
     * SameSite is bypassed.
     */
    static final String CSRF_HEADER = "X-Requested-With";

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final CurrentUserService currentUserService;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;
    private final AuthProperties authProperties;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return withSession(HttpStatus.CREATED, authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        String ip = ClientIp.of(servletRequest);
        rateLimiter.enforce("login-ip", ip, rateLimitProperties.getLogin());
        rateLimiter.enforce("login-account", ip + ":" + emailKey(request.email()), rateLimitProperties.getLogin());

        return withSession(HttpStatus.OK, authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        requireCsrfHeader(request);
        return withSession(HttpStatus.OK, authService.refresh(readRefreshCookie(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        requireCsrfHeader(request);
        authService.logout(readRefreshCookie(request));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    @PostMapping("/logout-all")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logoutAll(Authentication authentication) {
        authService.logoutEverywhere(currentUserService.getUserId(authentication));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody TokenRequest request) {
        emailVerificationService.verify(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> resendVerification(Authentication authentication) {
        Long userId = currentUserService.getUserId(authentication);
        rateLimiter.enforce("resend-verification", String.valueOf(userId), rateLimitProperties.getForgotPassword());
        emailVerificationService.resend(userId);
        return ResponseEntity.accepted().build();
    }

    /** Always 202, whether or not the email has an account. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        rateLimiter.enforce("forgot-ip", ClientIp.of(servletRequest), rateLimitProperties.getForgotPassword());

        RateLimiter.Decision perEmail = rateLimiter.tryConsume(
                "forgot-email", emailKey(request.email()), rateLimitProperties.getForgotPassword());
        if (perEmail.allowed()) {
            passwordResetService.requestReset(request.email());
        }

        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    private ResponseEntity<AuthResponse> withSession(HttpStatus status, AuthService.AuthSession session) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), session.refreshTtl()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(session.response());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(authProperties.getRefreshCookieName(), value)
                .httpOnly(true)
                .secure(authProperties.isRefreshCookieSecure())
                .sameSite("Strict")
                .path(authProperties.getRefreshCookiePath())
                .maxAge(maxAge)
                .build();
    }

    private String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (var cookie : request.getCookies()) {
            if (authProperties.getRefreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static void requireCsrfHeader(HttpServletRequest request) {
        String header = request.getHeader(CSRF_HEADER);
        if (header == null || header.isBlank()) {
            throw new InvalidCredentialsException("Missing " + CSRF_HEADER + " header");
        }
    }

    /** Hashed so raw email addresses never appear in Redis keys. */
    private static String emailKey(String email) {
        return SecureTokens.sha256Hex(email.trim().toLowerCase(Locale.ROOT)).substring(0, 32);
    }
}
