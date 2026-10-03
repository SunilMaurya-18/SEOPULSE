package com.seopulse.abuse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisposableEmailCheckerTest {

    @Test
    void refusesKnownThrowawayDomainsAndTheirSubdomains() {
        DisposableEmailChecker checker = new DisposableEmailChecker(new AbuseProperties());

        assertThat(checker.isDisposable("someone@mailinator.com")).isTrue();
        assertThat(checker.isDisposable("Someone@YOPMAIL.COM")).isTrue();
        assertThat(checker.isDisposable("someone@inbox.guerrillamail.com")).isTrue();
        assertThat(checker.isDisposable("someone@gmail.com")).isFalse();
        assertThat(checker.isDisposable("someone@notmailinator.com")).isFalse();
        assertThatThrownBy(() -> checker.requireAllowed("x@10minutemail.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("permanent email");
    }

    @Test
    void honoursExtraDomainsAndCanBeTurnedOff() {
        AbuseProperties properties = new AbuseProperties();
        properties.setExtraBlockedDomains(List.of("Spam.Example"));
        assertThat(new DisposableEmailChecker(properties).isDisposable("a@spam.example")).isTrue();

        properties.setBlockDisposableEmails(false);
        assertThat(new DisposableEmailChecker(properties).isDisposable("a@mailinator.com")).isFalse();
    }
}
