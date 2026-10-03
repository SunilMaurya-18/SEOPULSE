package com.seopulse.quickcheck;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "seopulse.quick-check")
@Getter
@Setter
public class QuickCheckProperties {

    private int maxPages = 5;

    /** No new request starts after this; the response also waits for requests in flight. */
    private Duration budget = Duration.ofSeconds(15);

    /** Checks running at once on one API instance; further requests get 429. */
    private int maxConcurrent = 4;

    /** Issue types returned, most severe first. */
    private int maxIssues = 8;
}
