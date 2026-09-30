package com.seopulse.auth.dto;

/**
 * The refresh token is never in the body; it travels in an HttpOnly cookie.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String name,
        String email,
        String role,
        boolean emailVerified,
        /* Whether unverified accounts are blocked from starting audits. */
        boolean emailVerificationRequired
) {
}
