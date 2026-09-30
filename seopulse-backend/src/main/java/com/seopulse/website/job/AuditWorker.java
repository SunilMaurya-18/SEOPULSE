package com.seopulse.website.job;

import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditStatus;
import com.seopulse.website.repository.AuditRepository;
import com.seopulse.website.seo.service.AuditAnalysisService;
import com.seopulse.website.service.AuditCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditWorker {

    private static final String CONSUMER_NAME = "worker-1";

    private final RedisTemplate<String, Object> redisTemplate;
    private final AuditRepository auditRepository;
    private final AuditQueue auditQueue;
    private final AuditCrawlerService auditCrawlerService;
    private final AuditAnalysisService auditAnalysisService;
    private final WorkerProperties workerProperties;

    /**
     * Reads the next available audit job from Redis Stream
     * and processes it.
     */
    public void processNextJob() {

        List<MapRecord<String, Object, Object>> records =
                redisTemplate
                        .opsForStream()
                        .read(
                                Consumer.from(
                                        AuditQueue.CONSUMER_GROUP,
                                        CONSUMER_NAME
                                ),
                                StreamOffset.create(
                                        AuditQueue.STREAM_KEY,
                                        ReadOffset.lastConsumed()
                                )
                        );

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            processRecord(record);
        }
    }


    /**
     * Processes one Redis Stream message.
     */
    private void processRecord(
            MapRecord<String, Object, Object> record
    ) {

        Object auditIdValue =
                record.getValue().get("auditId");

        /*
         * Invalid message:
         * There is no auditId.
         */
        if (auditIdValue == null) {

            log.error(
                    "Audit job does not contain auditId: recordId={}",
                    record.getId()
            );

            acknowledge(record);
            return;
        }


        Long auditId;

        try {

            auditId =
                    Long.parseLong(
                            auditIdValue.toString()
                    );

        } catch (NumberFormatException ex) {

            log.error(
                    "Invalid auditId: recordId={}, value={}",
                    record.getId(),
                    auditIdValue
            );

            acknowledge(record);
            return;
        }


        log.info(
                "Processing audit job: auditId={}, recordId={}",
                auditId,
                record.getId()
        );


        /*
         * Load audit from PostgreSQL.
         */
        Audit audit =
                auditRepository
                        .findByIdWithWebsite(auditId)
                        .orElse(null);

        if (audit == null) {

            log.info(
                    "Ignoring Redis audit job because audit was deleted: auditId={}",
                    auditId
            );

            acknowledge(record);

            return;
        }

        /*
         * Idempotency protection.
         *
         * Only QUEUED audits should be processed.
         *
         * If the same Redis message is delivered again
         * after the audit has already moved to CRAWLING,
         * ANALYZING, COMPLETED or FAILED, we skip it.
         */
        if (audit.getStatus() != AuditStatus.QUEUED) {

            log.warn(
                    "Skipping audit {} because status is {}",
                    auditId,
                    audit.getStatus()
            );

            acknowledge(record);
            return;
        }


        try {

            audit.setStatus(AuditStatus.CRAWLING);
            audit.setStartedAt(Instant.now());

            auditRepository.save(audit);

            log.info(
                    "Audit {} moved to CRAWLING",
                    auditId
            );

            auditCrawlerService.crawlAudit(auditId);

            auditAnalysisService.analyzeAudit(auditId);

            acknowledge(record);

        } catch (Exception ex) {

            log.error(
                    "Failed to process audit: auditId={}",
                    auditId,
                    ex
            );


            /*
             * Try to retry the audit.
             */
            boolean handled =
                    handleFailure(audit);


            /*
             * Only acknowledge the original message
             * when the failure has been handled successfully.
             *
             * If retry enqueue failed, we DON'T acknowledge.
             *
             * Redis will keep the message in the
             * Pending Entries List (PEL).
             */
            if (handled) {
                acknowledge(record);
            }
        }
    }


    /**
     * Handles an audit processing failure.
     * <p>
     * Returns:
     * <p>
     * true  -> failure was handled successfully
     * false -> message should remain pending
     */
    private boolean handleFailure(Audit audit) {

        int retryCount =
                audit.getRetryCount() + 1;

        audit.setRetryCount(retryCount);


        /*
         * Maximum retry attempts reached.
         */
        if (retryCount >= audit.getMaxRetries()) {

            audit.setStatus(AuditStatus.FAILED);

            audit.setCompletedAt(
                    Instant.now()
            );

            audit.setErrorMessage(
                    "Audit processing failed after maximum retries"
            );

            auditRepository.save(audit);


            log.error(
                    "Audit {} permanently failed after {} retries",
                    audit.getId(),
                    retryCount
            );


            /*
             * The failure has been permanently handled.
             * Therefore the current Redis message can be ACKed.
             */
            return true;
        }


        /*
         * Retry is still available.
         */
        audit.setStatus(AuditStatus.QUEUED);

        audit.setErrorMessage(
                "Audit processing failed. Retry scheduled."
        );

        auditRepository.save(audit);


        try {

            /*
             * Put the audit back into the Redis Stream.
             */
            String recordId =
                    auditQueue.enqueue(
                            audit.getId()
                    );


            log.warn(
                    "Audit {} retry scheduled: " +
                            "attempt {}/{}, recordId={}",
                    audit.getId(),
                    retryCount,
                    audit.getMaxRetries(),
                    recordId
            );


            /*
             * Retry message was successfully created.
             */
            return true;

        } catch (Exception ex) {

            log.error(
                    "Failed to enqueue retry for audit {}",
                    audit.getId(),
                    ex
            );


            /*
             * IMPORTANT:
             *
             * Do NOT ACK the current message.
             *
             * It remains in Redis PEL and can be recovered
             * by recoverPendingJobs().
             */
            return false;
        }
    }


    /**
     * Acknowledges a successfully handled Redis message.
     */
    private void acknowledge(
            MapRecord<String, Object, Object> record
    ) {

        redisTemplate
                .opsForStream()
                .acknowledge(
                        AuditQueue.CONSUMER_GROUP,
                        record
                );


        log.debug(
                "Audit job acknowledged: recordId={}",
                record.getId()
        );
    }


    /**
     * Recovers Redis Stream messages that have been
     * pending for at least the configured interval.
     * <p>
     * This protects against worker crashes.
     */
    public void recoverPendingJobs() {

        var pendingMessages =
                redisTemplate
                        .opsForStream()
                        .pending(
                                AuditQueue.STREAM_KEY,
                                AuditQueue.CONSUMER_GROUP,
                                Range.unbounded(),
                                workerProperties.getPendingRecoveryLimit(),
                                Duration.ofMinutes(
                                        workerProperties.getPendingRecoveryIntervalMinutes()
                                )
                        );


        if (pendingMessages == null
                || pendingMessages.isEmpty()) {

            return;
        }


        /*
         * Iterate through pending messages.
         */
        for (var pendingMessage : pendingMessages) {

            try {

                /*
                 * Transfer ownership of the pending
                 * message to this worker.
                 */
                List<MapRecord<String, Object, Object>> claimedRecords =
                        redisTemplate
                                .opsForStream()
                                .claim(
                                        AuditQueue.STREAM_KEY,
                                        AuditQueue.CONSUMER_GROUP,
                                        CONSUMER_NAME,
                                        Duration.ofMinutes(
                                                workerProperties.getPendingRecoveryIntervalMinutes()
                                        ),
                                        pendingMessage.getId()
                                );


                if (claimedRecords == null
                        || claimedRecords.isEmpty()) {

                    continue;
                }


                for (MapRecord<String, Object, Object> record
                        : claimedRecords) {

                    log.warn(
                            "Recovered pending audit job: " +
                                    "recordId={}",
                            record.getId()
                    );


                    processRecord(record);
                }

            } catch (Exception ex) {

                log.error(
                        "Failed to recover pending audit job: " +
                                "recordId={}",
                        pendingMessage.getId(),
                        ex
                );
            }
        }
    }
}