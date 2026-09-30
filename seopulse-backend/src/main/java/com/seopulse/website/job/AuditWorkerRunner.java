package com.seopulse.website.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Consumes audit jobs from the Redis stream. Each process registers a
 * unique consumer name, so any number of worker containers can share the
 * consumer group.
 */
@Component
@Profile("worker")
@Slf4j
public class AuditWorkerRunner implements SmartLifecycle {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AuditWorker auditWorker;
    private final WorkerProperties properties;
    private final String consumerName;

    private final List<Thread> loops = new ArrayList<>();
    private volatile boolean running;

    public AuditWorkerRunner(
            RedisTemplate<String, Object> redisTemplate,
            AuditWorker auditWorker,
            WorkerProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.auditWorker = auditWorker;
        this.properties = properties;
        this.consumerName = hostname() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String consumerName() {
        return consumerName;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;

        int concurrency = Math.max(1, properties.getConcurrency());
        for (int i = 0; i < concurrency; i++) {
            loops.add(Thread.ofPlatform().name("audit-worker-" + i).daemon(false).start(this::consumeLoop));
        }

        log.info("Audit worker started: consumer={}, concurrency={}", consumerName, concurrency);
    }

    @Override
    public synchronized void stop() {
        running = false;
        loops.forEach(Thread::interrupt);
        for (Thread loop : loops) {
            try {
                loop.join(Duration.ofSeconds(30));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        loops.clear();
        log.info("Audit worker stopped: consumer={}", consumerName);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void consumeLoop() {

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                        Consumer.from(AuditQueue.CONSUMER_GROUP, consumerName),
                        StreamReadOptions.empty()
                                .count(1)
                                .block(Duration.ofSeconds(properties.getBlockTimeoutSeconds())),
                        StreamOffset.create(AuditQueue.STREAM_KEY, ReadOffset.lastConsumed())
                );

                if (records == null) {
                    continue;
                }

                for (MapRecord<String, Object, Object> record : records) {
                    handle(record);
                }
            } catch (RuntimeException ex) {
                if (!running) {
                    return;
                }
                log.error("Audit worker loop error; backing off", ex);
                sleepQuietly(Duration.ofSeconds(5));
            }
        }
    }

    private void handle(MapRecord<String, Object, Object> record) {
        if (auditWorker.handle(record)) {
            redisTemplate.opsForStream().acknowledge(AuditQueue.CONSUMER_GROUP, record);
        }
    }

    /**
     * Claims messages that a dead consumer left pending and processes them.
     */
    public void recoverPendingJobs() {

        Duration minIdle = properties.getPendingRecoveryIdle();

        PendingMessages pending = redisTemplate.opsForStream().pending(
                AuditQueue.STREAM_KEY,
                AuditQueue.CONSUMER_GROUP,
                Range.unbounded(),
                properties.getPendingRecoveryLimit(),
                minIdle
        );

        if (pending == null || pending.isEmpty()) {
            return;
        }

        for (var message : pending) {
            try {
                List<MapRecord<String, Object, Object>> claimed = redisTemplate.opsForStream().claim(
                        AuditQueue.STREAM_KEY,
                        AuditQueue.CONSUMER_GROUP,
                        consumerName,
                        minIdle,
                        message.getId()
                );

                for (MapRecord<String, Object, Object> record : claimed) {
                    log.warn("Recovered pending audit job: recordId={}, previousConsumer={}",
                            record.getId(), message.getConsumerName());
                    handle(record);
                }
            } catch (RuntimeException ex) {
                log.error("Failed to recover pending audit job: recordId={}", message.getId(), ex);
            }
        }
    }

    /**
     * Removes consumers left behind by stopped containers. Only consumers
     * with nothing pending are removed, so no job is ever orphaned.
     */
    public void cleanupIdleConsumers() {

        StreamInfo.XInfoConsumers consumers =
                redisTemplate.opsForStream().consumers(AuditQueue.STREAM_KEY, AuditQueue.CONSUMER_GROUP);

        long idleLimitMs = Duration.ofHours(properties.getConsumerCleanupIdleHours()).toMillis();

        consumers.forEach(consumer -> {
            if (!consumer.consumerName().equals(consumerName)
                    && consumer.pendingCount() == 0
                    && consumer.idleTimeMs() > idleLimitMs) {
                redisTemplate.opsForStream().deleteConsumer(
                        AuditQueue.STREAM_KEY,
                        Consumer.from(AuditQueue.CONSUMER_GROUP, consumer.consumerName())
                );
                log.info("Removed idle stream consumer: {}", consumer.consumerName());
            }
        });
    }

    private static String hostname() {
        String env = System.getenv("HOSTNAME");
        if (env != null && !env.isBlank()) {
            return env;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            return "worker";
        }
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
