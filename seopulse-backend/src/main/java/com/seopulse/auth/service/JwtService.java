package com.seopulse.auth.service;

import com.seopulse.auth.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final AuthProperties authProperties;

    public String generateToken(Long userId, String email, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiresAt(now.plus(authProperties.getAccessTokenTtl()))
                .build();
        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();
        return jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

    public long accessTokenTtlSeconds() {
        return authProperties.getAccessTokenTtl().toSeconds();
    }
}
