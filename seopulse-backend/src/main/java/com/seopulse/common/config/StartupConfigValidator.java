package com.seopulse.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Refuses to start with configuration that is unsafe to run,
 * instead of failing later at request time.
 */
@Component
public class StartupConfigValidator {

    static final int MIN_JWT_SECRET_BYTES = 32;

    static final String DEV_DB_PASSWORD = "seopulse_dev_password";

    private final Environment environment;
    private final String jwtSecret;
    private final String dbPassword;
    private final List<String> allowedOrigins;
    private final boolean allowPrivateNetworks;
    private final String botInfoUrl;

    public StartupConfigValidator(
            Environment environment,
            @Value("${jwt.secret:}") String jwtSecret,
            @Value("${spring.datasource.password:}") String dbPassword,
            @Value("${seopulse.cors.allowed-origins:}") List<String> allowedOrigins,
            @Value("${seopulse.crawler.allow-private-networks:false}") boolean allowPrivateNetworks,
            @Value("${seopulse.crawler.bot-info-url:}") String botInfoUrl
    ) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.dbPassword = dbPassword;
        this.allowedOrigins = allowedOrigins;
        this.allowPrivateNetworks = allowPrivateNetworks;
        this.botInfoUrl = botInfoUrl;
    }

    @PostConstruct
    void validate() {

        List<String> problems = new ArrayList<>();

        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
            problems.add("JWT_SECRET must be at least " + MIN_JWT_SECRET_BYTES + " bytes");
        }

        if (environment.acceptsProfiles(Profiles.of("prod"))) {

            if (dbPassword.isBlank() || DEV_DB_PASSWORD.equals(dbPassword)) {
                problems.add("DB_PASSWORD must be set to a non-default value in prod");
            }

            List<String> origins = allowedOrigins.stream()
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .toList();

            if (origins.isEmpty()) {
                problems.add("CORS_ALLOWED_ORIGINS must be set in prod");
            }

            origins.stream()
                    .filter(origin -> origin.equals("*") || !origin.startsWith("https://"))
                    .forEach(origin -> problems.add(
                            "CORS origin must be an explicit https:// origin in prod: " + origin
                    ));

            if (allowPrivateNetworks) {
                problems.add("seopulse.crawler.allow-private-networks must be false in prod");
            }

            if (!botInfoUrl.startsWith("https://")) {
                problems.add("SEOPULSE_BOT_INFO_URL must be an https:// URL in prod");
            }
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Invalid configuration: " + String.join("; ", problems)
            );
        }
    }
}
