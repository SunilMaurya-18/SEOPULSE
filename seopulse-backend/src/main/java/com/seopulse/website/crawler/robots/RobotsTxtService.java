package com.seopulse.website.crawler.robots;

import com.seopulse.website.crawler.CrawlerProperties;
import com.seopulse.website.crawler.net.CrawlerHttpClient;
import com.seopulse.website.crawler.net.CrawlerHttpClient.FetchGate;
import com.seopulse.website.crawler.net.FetchResponse;
import com.seopulse.website.crawler.robots.RobotsTxtCache.CachedRobotsTxt;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRules.RobotRulesMode;
import crawlercommons.robots.SimpleRobotRulesParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Fetches and interprets robots.txt following RFC 9309:
 * <ul>
 *   <li>2xx: rules are parsed and cached;</li>
 *   <li>4xx (except 429): no rules apply, everything is allowed (cached);</li>
 *   <li>429, 5xx or unreachable: complete disallow (not cached).</li>
 * </ul>
 */
@Service
@Slf4j
public class RobotsTxtService {

    private static final int MAX_ROBOTS_REDIRECTS = 5;
    private static final long MAX_ROBOTS_BYTES = 512 * 1024;

    private final CrawlerProperties properties;
    private final CrawlerHttpClient httpClient;
    private final RobotsTxtCache cache;
    private final SimpleRobotRulesParser parser;

    public RobotsTxtService(
            CrawlerProperties properties,
            CrawlerHttpClient httpClient,
            RobotsTxtCache cache
    ) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.cache = cache;
        // Crawl-delay is clamped by the crawler; never let the parser turn a long delay into "disallow all".
        this.parser = new SimpleRobotRulesParser(Long.MAX_VALUE, 5);
    }

    public BaseRobotRules rulesFor(URI uri, FetchGate gate) throws InterruptedException {

        if (!properties.isRespectRobotsTxt()) {
            return new SimpleRobotRules(RobotRulesMode.ALLOW_ALL);
        }

        String origin = origin(uri);
        String robotsUrl = origin + "/robots.txt";

        Optional<CachedRobotsTxt> cached = cache.get(origin);

        if (cached.isPresent()) {
            return interpret(robotsUrl, cached.get());
        }

        CachedRobotsTxt fetched;

        try {
            FetchResponse response = httpClient.fetchFollowingRedirects(
                    URI.create(robotsUrl),
                    "text/plain,*/*;q=0.5",
                    MAX_ROBOTS_BYTES,
                    MAX_ROBOTS_REDIRECTS,
                    gate
            );

            if (response.isRedirect()) {
                log.info("robots.txt redirected too many times, allowing all: origin={}", origin);
                fetched = new CachedRobotsTxt(404, "");
            } else if (response.tooLarge()) {
                log.info("robots.txt larger than {} bytes, allowing all: origin={}", MAX_ROBOTS_BYTES, origin);
                fetched = new CachedRobotsTxt(404, "");
            } else {
                fetched = new CachedRobotsTxt(
                        response.status(),
                        new String(response.body(), StandardCharsets.UTF_8)
                );
            }
        } catch (IOException ex) {
            log.info("robots.txt unreachable, disallowing crawl: origin={}, error={}", origin, ex.getMessage());
            return new SimpleRobotRules(RobotRulesMode.ALLOW_NONE);
        }

        if (isCacheable(fetched.status())) {
            cache.put(origin, fetched, Duration.ofHours(properties.getRobotsCacheTtlHours()));
        }

        return interpret(robotsUrl, fetched);
    }

    private BaseRobotRules interpret(String robotsUrl, CachedRobotsTxt robotsTxt) {

        int status = robotsTxt.status();

        if (status >= 200 && status < 300) {
            return parser.parseContent(
                    robotsUrl,
                    robotsTxt.body().getBytes(StandardCharsets.UTF_8),
                    "text/plain",
                    List.of(properties.getBotName().toLowerCase())
            );
        }

        if (status >= 400 && status < 500 && status != 429) {
            return new SimpleRobotRules(RobotRulesMode.ALLOW_ALL);
        }

        return new SimpleRobotRules(RobotRulesMode.ALLOW_NONE);
    }

    private static boolean isCacheable(int status) {
        return (status >= 200 && status < 300)
                || (status >= 400 && status < 500 && status != 429);
    }

    public static String origin(URI uri) {

        String scheme = uri.getScheme().toLowerCase();
        int port = uri.getPort();
        boolean defaultPort = port == -1
                || (port == 80 && scheme.equals("http"))
                || (port == 443 && scheme.equals("https"));

        return scheme + "://" + uri.getHost().toLowerCase() + (defaultPort ? "" : ":" + port);
    }
}
