package com.seopulse.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartupConfigValidatorTest {

    private static final String STRONG_SECRET =
            "a-sufficiently-long-secret-for-hs256-signing";

    @Test
    void acceptsDevDefaultsWithStrongSecret() {

        StartupConfigValidator validator = validator(
                "dev",
                STRONG_SECRET,
                StartupConfigValidator.DEV_DB_PASSWORD,
                List.of()
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsShortJwtSecretInAnyProfile() {

        StartupConfigValidator validator = validator(
                "dev",
                "too-short",
                "whatever",
                List.of()
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void rejectsDefaultDbPasswordInProd() {

        StartupConfigValidator validator = validator(
                "prod",
                STRONG_SECRET,
                StartupConfigValidator.DEV_DB_PASSWORD,
                List.of("https://app.seopulse.io")
        );

        assertThatThrownBy(validator::validate)
                .hasMessageContaining("DB_PASSWORD");
    }

    @Test
    void rejectsMissingOrInsecureCorsOriginsInProd() {

        assertThatThrownBy(validator("prod", STRONG_SECRET, "prod-password", List.of())::validate)
                .hasMessageContaining("CORS_ALLOWED_ORIGINS");

        assertThatThrownBy(validator("prod", STRONG_SECRET, "prod-password", List.of("*"))::validate)
                .hasMessageContaining("https://");

        assertThatThrownBy(validator("prod", STRONG_SECRET, "prod-password",
                List.of("http://app.seopulse.io"))::validate)
                .hasMessageContaining("https://");
    }

    @Test
    void acceptsSecureProdConfiguration() {

        StartupConfigValidator validator = validator(
                "prod",
                STRONG_SECRET,
                "prod-password",
                List.of("https://app.seopulse.io")
        );

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsCrawlerTestSettingsInProd() {

        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        StartupConfigValidator validator = new StartupConfigValidator(
                environment,
                STRONG_SECRET,
                "prod-password",
                List.of("https://app.seopulse.io"),
                true,
                "http://localhost:5173/bot"
        );

        assertThatThrownBy(validator::validate)
                .hasMessageContaining("allow-private-networks")
                .hasMessageContaining("SEOPULSE_BOT_INFO_URL");
    }

    private static StartupConfigValidator validator(
            String profile,
            String jwtSecret,
            String dbPassword,
            List<String> origins
    ) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);

        return new StartupConfigValidator(
                environment,
                jwtSecret,
                dbPassword,
                origins,
                false,
                "https://app.seopulse.io/bot"
        );
    }
}
