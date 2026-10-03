package com.seopulse.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "seopulse.auth")
@Getter
@Setter
public class AuthProperties {

    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(30);

    private String refreshCookieName = "seopulse_refresh";
    private String refreshCookiePath = "/api/v1/auth";
    private boolean refreshCookieSecure = true;

    private Duration emailVerificationTtl = Duration.ofHours(24);
    private Duration passwordResetTtl = Duration.ofHours(1);
    private boolean requireEmailVerification = true;

    private int maxFailedLogins = 10;
    private Duration lockoutDuration = Duration.ofMinutes(15);

    private int passwordMinLength = 10;
    private boolean breachedPasswordCheck = true;
    private String breachedPasswordApiUrl = "https://api.pwnedpasswords.com/range/";
    private Duration breachedPasswordTimeout = Duration.ofSeconds(3);

    /** Public URL of the frontend, used to build links in emails. */
    private String appBaseUrl = "http://localhost:5173";

    /** OAuth client ID for "Sign in with Google". Blank disables it. */
    private String googleClientId = "";
    private String googleJwkSetUri = "https://www.googleapis.com/oauth2/v3/certs";
}
