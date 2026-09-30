package com.seopulse.common.ratelimit;

import com.seopulse.common.exception.RateLimitExceededException;
import com.seopulse.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RateLimiter rateLimiter;

    @Test
    void bucketsAreSharedInRedisAndReportRetryAfter() {

        String identity = UUID.randomUUID().toString();
        List<RateLimitProperties.Limit> limits = List.of(new RateLimitProperties.Limit(2, Duration.ofMinutes(1)));

        RateLimiter.Decision first = rateLimiter.tryConsume("test", identity, limits);
        RateLimiter.Decision second = rateLimiter.tryConsume("test", identity, limits);
        RateLimiter.Decision third = rateLimiter.tryConsume("test", identity, limits);

        assertThat(first.allowed()).isTrue();
        assertThat(first.remaining()).isEqualTo(1);
        assertThat(second.allowed()).isTrue();
        assertThat(third.allowed()).isFalse();
        assertThat(third.limit()).isEqualTo(2);
        assertThat(third.retryAfterSeconds()).isBetween(1L, 60L);

        // Independent identities have independent buckets.
        assertThat(rateLimiter.tryConsume("test", identity + "-other", limits).allowed()).isTrue();
    }

    @Test
    void enforceThrowsWhenExhausted() {

        String identity = UUID.randomUUID().toString();
        List<RateLimitProperties.Limit> limits = List.of(new RateLimitProperties.Limit(1, Duration.ofHours(1)));

        rateLimiter.enforce("test-enforce", identity, limits);

        assertThatThrownBy(() -> rateLimiter.enforce("test-enforce", identity, limits))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void theStrictestOfSeveralLimitsApplies() {

        String identity = UUID.randomUUID().toString();
        List<RateLimitProperties.Limit> limits = List.of(
                new RateLimitProperties.Limit(10, Duration.ofMinutes(1)),
                new RateLimitProperties.Limit(3, Duration.ofHours(1))
        );

        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryConsume("test-multi", identity, limits).allowed()).isTrue();
        }
        assertThat(rateLimiter.tryConsume("test-multi", identity, limits).allowed()).isFalse();
    }
}
