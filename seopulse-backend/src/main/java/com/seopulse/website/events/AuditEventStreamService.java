package com.seopulse.website.events;

import com.seopulse.website.dto.AuditResponse;
import com.seopulse.website.service.AuditService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pushes audit status changes to browsers over Server-Sent Events.
 * One Redis pattern subscription per API instance fans out to the local
 * emitters, so the number of Redis subscriptions does not grow with the
 * number of viewers.
 */
@Service
@Slf4j
public class AuditEventStreamService implements MessageListener {

    static final Duration EMITTER_TIMEOUT = Duration.ofMinutes(30);

    private final AuditService auditService;
    private final RedisMessageListenerContainer container;
    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public AuditEventStreamService(AuditService auditService, RedisConnectionFactory connectionFactory) {
        this.auditService = auditService;
        this.container = new RedisMessageListenerContainer();
        this.container.setConnectionFactory(connectionFactory);
    }

    @PostConstruct
    void start() {
        container.addMessageListener(this, new PatternTopic(AuditEventPublisher.CHANNEL_PREFIX + "*"));
        container.afterPropertiesSet();
        container.start();
    }

    @PreDestroy
    void stop() throws Exception {
        emitters.values().forEach(set -> set.forEach(SseEmitter::complete));
        emitters.clear();
        container.stop();
        container.destroy();
    }

    /**
     * Opens a stream for an audit the caller already owns. The current
     * state is sent immediately; the stream ends at a final status.
     */
    public SseEmitter subscribe(AuditResponse current) {

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT.toMillis());
        Long auditId = current.id();

        if (current.status().isActive()) {
            // Registered before the first send so no status change falls in between.
            emitters.computeIfAbsent(auditId, id -> ConcurrentHashMap.newKeySet()).add(emitter);

            Runnable remove = () -> removeEmitter(auditId, emitter);
            emitter.onCompletion(remove);
            emitter.onTimeout(remove);
            emitter.onError(error -> remove.run());
        }

        if (send(emitter, current) && !current.status().isActive()) {
            emitter.complete();
        }

        return emitter;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {

        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        Long auditId;
        try {
            auditId = Long.parseLong(channel.substring(AuditEventPublisher.CHANNEL_PREFIX.length()));
        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
            return;
        }

        Set<SseEmitter> subscribers = emitters.get(auditId);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        auditService.findAudit(auditId).ifPresent(audit -> {
            for (SseEmitter emitter : subscribers) {
                if (send(emitter, audit) && !audit.status().isActive()) {
                    emitter.complete();
                }
            }
        });
    }

    /** Keeps proxies from closing idle connections. */
    @Scheduled(fixedDelay = 20_000)
    public void heartbeat() {
        emitters.forEach((auditId, subscribers) -> subscribers.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("keepalive"));
            } catch (IOException | IllegalStateException ex) {
                removeEmitter(auditId, emitter);
            }
        }));
    }

    int subscriberCount() {
        return emitters.values().stream().mapToInt(Set::size).sum();
    }

    private boolean send(SseEmitter emitter, AuditResponse audit) {
        try {
            emitter.send(SseEmitter.event()
                    .name("audit")
                    .id(audit.status().name())
                    .data(audit, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException | IllegalStateException ex) {
            log.debug("SSE client disconnected: auditId={}", audit.id());
            emitter.completeWithError(ex);
            return false;
        }
    }

    private void removeEmitter(Long auditId, SseEmitter emitter) {
        emitters.computeIfPresent(auditId, (id, set) -> {
            set.remove(emitter);
            return set.isEmpty() ? null : set;
        });
    }
}
