package com.seopulse.auth.service;

import com.seopulse.auth.config.AuthProperties;
import com.seopulse.common.exception.WeakPasswordException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final AuthProperties properties = new AuthProperties();

    @Test
    void acceptsLongUniquePassword() {
        assertThatCode(() -> policy(false).validate("violet-anchor-river", "jane@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsShortPassword() {
        assertThatThrownBy(() -> policy(false).validate("short", "jane@example.com"))
                .isInstanceOf(WeakPasswordException.class)
                .hasMessageContaining("at least 10");
    }

    @Test
    void rejectsPasswordsBeyondBcryptLimit() {
        // 25 three-byte characters = 75 bytes.
        String longPassword = "\u20ac".repeat(25);
        assertThatThrownBy(() -> policy(false).validate(longPassword, "jane@example.com"))
                .hasMessageContaining("72 bytes");
    }

    @Test
    void rejectsPasswordContainingEmailName() {
        assertThatThrownBy(() -> policy(false).validate("JaneDoe-2026!", "janedoe@example.com"))
                .hasMessageContaining("email");
    }

    @Test
    void shortEmailNamesAreNotMatched() {
        assertThatCode(() -> policy(false).validate("bob-likes-long-passwords", "bob@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsBreachedPassword() {
        assertThatThrownBy(() -> policy(true).validate("password12345", "jane@example.com"))
                .hasMessageContaining("data breach");
    }

    private PasswordPolicy policy(boolean breached) {
        return new PasswordPolicy(properties, password -> breached);
    }
}
