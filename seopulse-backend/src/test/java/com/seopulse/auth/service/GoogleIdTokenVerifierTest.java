package com.seopulse.auth.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.seopulse.common.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleIdTokenVerifierTest {

    private static final String CLIENT_ID = "client-123.apps.googleusercontent.com";

    private final KeyPair keys = rsaKeys();
    private final GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier(
            CLIENT_ID, NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build());

    @Test
    void returnsTheIdentityFromAValidToken() throws Exception {
        GoogleIdTokenVerifier.GoogleIdentity identity =
                verifier.verify(token("https://accounts.google.com", CLIENT_ID, Instant.now().plusSeconds(300)));

        assertThat(identity.subject()).isEqualTo("google-sub-1");
        assertThat(identity.email()).isEqualTo("ada@example.com");
        assertThat(identity.emailVerified()).isTrue();
        assertThat(identity.name()).isEqualTo("Ada Lovelace");
    }

    @Test
    void rejectsTokensForOtherAppsIssuersOrThatExpired() throws Exception {
        Instant later = Instant.now().plusSeconds(300);

        assertThatThrownBy(() -> verifier.verify(token("https://accounts.google.com", "someone-else", later)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> verifier.verify(token("https://evil.example", CLIENT_ID, later)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> verifier.verify(token("accounts.google.com", CLIENT_ID, Instant.now().minusSeconds(600))))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> verifier.verify("not-a-jwt"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void isOffWithoutAClientId() {
        GoogleIdTokenVerifier disabled = new GoogleIdTokenVerifier("", null);

        assertThat(disabled.isEnabled()).isFalse();
        assertThatThrownBy(() -> disabled.verify("anything")).isInstanceOf(IllegalArgumentException.class);
    }

    private String token(String issuer, String audience, Instant expiresAt) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("google-sub-1")
                .issueTime(Date.from(expiresAt.minusSeconds(3600)))
                .expirationTime(Date.from(expiresAt))
                .claim("email", "ada@example.com")
                .claim("email_verified", true)
                .claim("name", "Ada Lovelace")
                .build());
        jwt.sign(new RSASSASigner(keys.getPrivate()));
        return jwt.serialize();
    }

    private static KeyPair rsaKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
