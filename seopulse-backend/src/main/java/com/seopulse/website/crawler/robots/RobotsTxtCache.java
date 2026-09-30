package com.seopulse.website.crawler.robots;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache of raw robots.txt fetch results, keyed by origin
 * (scheme://host[:port]).
 */
public interface RobotsTxtCache {

    Optional<CachedRobotsTxt> get(String origin);

    void put(String origin, CachedRobotsTxt robotsTxt, Duration ttl);

    record CachedRobotsTxt(int status, String body) {
    }
}
