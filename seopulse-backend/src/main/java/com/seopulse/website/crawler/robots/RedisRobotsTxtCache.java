package com.seopulse.website.crawler.robots;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@Slf4j
public class RedisRobotsTxtCache implements RobotsTxtCache {

    private static final String KEY_PREFIX = "seopulse:robots:";

    private final StringRedisTemplate redisTemplate;

    public RedisRobotsTxtCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<CachedRobotsTxt> get(String origin) {

        try {
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + origin);

            if (value == null) {
                return Optional.empty();
            }

            int newline = value.indexOf('\n');

            if (newline < 0) {
                return Optional.empty();
            }

            return Optional.of(new CachedRobotsTxt(
                    Integer.parseInt(value.substring(0, newline)),
                    value.substring(newline + 1)
            ));
        } catch (RuntimeException ex) {
            log.warn("robots.txt cache read failed: origin={}, error={}", origin, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String origin, CachedRobotsTxt robotsTxt, Duration ttl) {

        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + origin,
                    robotsTxt.status() + "\n" + robotsTxt.body(),
                    ttl
            );
        } catch (RuntimeException ex) {
            log.warn("robots.txt cache write failed: origin={}, error={}", origin, ex.getMessage());
        }
    }
}
