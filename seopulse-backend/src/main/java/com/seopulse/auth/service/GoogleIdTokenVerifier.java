package com.seopulse.auth.service;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.common.exception.InvalidCredentialsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Verifies ID tokens issued by Google Identity Services against Google's published keys. */
@Component
@Slf4j
public class GoogleIdTokenVerifier {

    public record GoogleIdentity(String subject, String email, boolean emailVerified, String name) {
    }

    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private final NimbusJwtDecoder decoder;

    @Autowired
    public GoogleIdTokenVerifier(AuthProperties properties) {
        this(properties.getGoogleClientId(), properties.getGoogleClientId().isBlank()
                ? null
                : NimbusJwtDecoder.withJwkSetUri(properties.getGoogleJwkSetUri()).build());
    }

    GoogleIdTokenVerifier(String clientId, NimbusJwtDecoder decoder) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.decoder = decoder;
        if (decoder != null) {
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuerAndAudience()));
        }
    }

    public boolean isEnabled() {
        return decoder != null && !clientId.isEmpty();
    }

    public GoogleIdentity verify(String idToken) {
        if (!isEnabled()) {
            throw new IllegalArgumentException("Google sign-in is not available.");
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException ex) {
            log.warn("Rejected Google ID token: {}", ex.getMessage());
            throw new InvalidCredentialsException("Google sign-in failed. Please try again.");
        }
        String email = jwt.getClaimAsString("email");
        if (jwt.getSubject() == null || email == null || email.isBlank()) {
            throw new InvalidCredentialsException("Google did not share an email address for this account.");
        }
        Object verified = jwt.getClaims().get("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(verified) || "true".equalsIgnoreCase(String.valueOf(verified));
        return new GoogleIdentity(jwt.getSubject(), email, emailVerified, jwt.getClaimAsString("name"));
    }

    private OAuth2TokenValidator<Jwt> issuerAndAudience() {
        return jwt -> {
            boolean issuerOk = ISSUERS.contains(jwt.getClaimAsString("iss"));
            boolean audienceOk = jwt.getAudience() != null && jwt.getAudience().contains(clientId);
            return issuerOk && audienceOk
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong issuer or audience", null));
        };
    }
}
