package com.seopulse.common.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "seopulse.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled = true;

    /** Per IP and per IP + email. */
    private List<Limit> login = List.of(new Limit(5, Duration.ofMinutes(1)), new Limit(20, Duration.ofHours(1)));

    /** Per IP. */
    private List<Limit> register = List.of(new Limit(5, Duration.ofHours(1)));

    /** Per IP and per email. */
    private List<Limit> forgotPassword = List.of(new Limit(3, Duration.ofHours(1)));

    /** Per user. */
    private List<Limit> auditCreate = List.of(new Limit(10, Duration.ofHours(1)));

    /** Mailing list sign-ups, per IP and per email. */
    private List<Limit> newsletter = List.of(new Limit(5, Duration.ofHours(1)));

    /** Data exports and account deletion attempts, per user. */
    private List<Limit> accountAction = List.of(new Limit(5, Duration.ofHours(1)));

    /** Free landing-page checks, per IP. */
    private List<Limit> quickCheck = List.of(new Limit(5, Duration.ofHours(1)), new Limit(15, Duration.ofDays(1)));

    /** Free landing-page checks, per checked host, so the crawler cannot be aimed at one site. */
    private List<Limit> quickCheckHost = List.of(new Limit(10, Duration.ofHours(1)));

    /** Per authenticated user, or per IP for anonymous requests. */
    private List<Limit> general = List.of(new Limit(300, Duration.ofMinutes(1)));

    public record Limit(long capacity, Duration period) {
    }
}
