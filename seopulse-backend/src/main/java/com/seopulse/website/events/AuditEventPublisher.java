package com.seopulse.website.events;

import com.seopulse.website.entity.AuditStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Announces audit status changes on Redis pub/sub so every API instance
 * can push them to its connected SSE clients. Best effort: clients fall
 * back to polling, so a lost message only delays the update.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

    public static final String CHANNEL_PREFIX = "seopulse:audit:events:";

    private final StringRedisTemplate redisTemplate;

    public void publish(Long auditId, AuditStatus status) {
        try {
            redisTemplate.convertAndSend(CHANNEL_PREFIX + auditId, status.name());
        } catch (RuntimeException ex) {
            log.warn("Failed to publish audit event: auditId={}, status={}, error={}", auditId, status, ex.toString());
        }
    }
}
