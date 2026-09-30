package com.seopulse.website.crawler;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "seopulse.crawler")
@Getter
@Setter
public class CrawlerProperties {
    private int maxPages = 500;
    private int maxDepth = 5;

    private int connectTimeoutMs = 10000;
    private int requestTimeoutMs = 15000;
    private int maxRedirects = 5;
    private int concurrency = 2;

    private long maxBodySizeBytes = 5_000_000;
    private long maxSitemapSizeBytes = 10_000_000;
    private int maxSitemaps = 10;

    /** Token matched against robots.txt User-agent groups. */
    private String botName = "SEOPulseBot";
    private String botInfoUrl = "https://seopulse.example.com/bot";
    private String userAgent = "SEOPulseBot/1.0 (+https://seopulse.example.com/bot)";

    private boolean respectRobotsTxt = true;
    private long robotsCacheTtlHours = 24;

    /** Minimum gap between two requests to the same host. */
    private long minDelayMs = 250;
    /** Upper bound applied to robots.txt Crawl-delay values. */
    private long maxCrawlDelayMs = 30_000;

    private int maxRetries = 2;
    private long defaultRetryAfterSeconds = 5;
    private long maxRetryAfterSeconds = 60;

    private long maxDurationMinutes = 20;

    private List<Integer> allowedPorts = new ArrayList<>(List.of(80, 443));

    /** Test-only escape hatch; rejected at startup in the prod profile. */
    private boolean allowPrivateNetworks = false;
}
