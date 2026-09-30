package com.seopulse.common.ratelimit;

import com.seopulse.common.exception.RateLimitExceededException;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConfigurationBuilder;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Distributed token buckets in Redis, shared by every API instance.
 * Fails open: if Redis is unavailable, requests are allowed rather than
 * turning a cache outage into a full outage.
 */
@Component
@Slf4j
public class RateLimiter {

    private static final String KEY_PREFIX = "seopulse:rl:";
    private static final Duration FAILURE_LOG_INTERVAL = Duration.ofMinutes(1);

    public record Decision(boolean allowed, long limit, long remaining, long retryAfterSeconds) {

        static Decision unlimited() {
            return new Decision(true, -1, -1, 0);
        }
    }

    private final RateLimitProperties properties;
    private final LettuceConnectionFactory connectionFactory;

    private volatile ProxyManager<String> proxyManager;
    private volatile StatefulRedisConnection<String, byte[]> connection;
    private volatile Instant lastFailureLog = Instant.EPOCH;

    public RateLimiter(RateLimitProperties properties, LettuceConnectionFactory connectionFactory) {
        this.properties = properties;
        this.connectionFactory = connectionFactory;
    }

    /**
     * @throws RateLimitExceededException when any of the limits is exhausted
     */
    public Decision enforce(String name, String identity, List<RateLimitProperties.Limit> limits) {
        Decision decision = tryConsume(name, identity, limits);
        if (!decision.allowed()) {
            throw new RateLimitExceededException(decision.limit(), decision.retryAfterSeconds());
        }
        return decision;
    }

    public Decision tryConsume(String name, String identity, List<RateLimitProperties.Limit> limits) {

        if (!properties.isEnabled() || limits == null || limits.isEmpty()) {
            return Decision.unlimited();
        }

        try {
            ConsumptionProbe probe = proxyManager()
                    .builder()
                    .build(KEY_PREFIX + name + ":" + identity, () -> configuration(limits))
                    .tryConsumeAndReturnRemaining(1);

            long limit = limits.stream().mapToLong(RateLimitProperties.Limit::capacity).min().orElse(0);

            if (probe.isConsumed()) {
                return new Decision(true, limit, probe.getRemainingTokens(), 0);
            }

            long retryAfter = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
            return new Decision(false, limit, 0, retryAfter);
        } catch (RuntimeException ex) {
            logFailure(ex);
            return Decision.unlimited();
        }
    }

    private static BucketConfiguration configuration(List<RateLimitProperties.Limit> limits) {
        ConfigurationBuilder builder = BucketConfiguration.builder();
        for (RateLimitProperties.Limit limit : limits) {
            builder.addLimit(bandwidth -> bandwidth
                    .capacity(limit.capacity())
                    .refillGreedy(limit.capacity(), limit.period()));
        }
        return builder.build();
    }

    private ProxyManager<String> proxyManager() {

        ProxyManager<String> manager = proxyManager;
        StatefulRedisConnection<String, byte[]> current = connection;
        if (manager != null && current != null && current.isOpen()) {
            return manager;
        }

        synchronized (this) {
            // Restarting the connection factory shuts down its client, which closes this connection.
            if (proxyManager == null || connection == null || !connection.isOpen()) {
                if (connection != null) {
                    connection.close();
                }
                AbstractRedisClient nativeClient = connectionFactory.getRequiredNativeClient();
                if (!(nativeClient instanceof RedisClient redisClient)) {
                    throw new IllegalStateException("Rate limiting requires a standalone Redis client");
                }
                connection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
                proxyManager = Bucket4jLettuce.casBasedBuilder(connection)
                        .expirationAfterWrite(
                                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10))
                        )
                        .build();
            }
            return proxyManager;
        }
    }

    private void logFailure(RuntimeException ex) {
        Instant now = Instant.now();
        if (Duration.between(lastFailureLog, now).compareTo(FAILURE_LOG_INTERVAL) > 0) {
            lastFailureLog = now;
            log.warn("Rate limiter unavailable; allowing requests: {}", ex.toString());
        }
    }

    @PreDestroy
    synchronized void close() {
        if (connection != null) {
            connection.close();
        }
    }
}
