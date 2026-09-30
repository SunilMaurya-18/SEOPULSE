package com.seopulse.website.job;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuditQueue {

    public static final String STREAM_KEY =
            "seopulse:audit:stream";

    public static final String CONSUMER_GROUP =
            "audit-workers";

    private final RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void initializeConsumerGroup() {

        try {

            redisTemplate
                    .opsForStream()
                    .createGroup(
                            STREAM_KEY,
                            ReadOffset.from("0-0"),
                            CONSUMER_GROUP
                    );

        } catch (Exception ex) {

            if (!isGroupAlreadyExists(ex)) {
                throw ex;
            }
        }
    }

    public String enqueue(Long auditId) {

        MapRecord<String, String, String> record =
                MapRecord.create(
                        STREAM_KEY,
                        Map.of(
                                "auditId",
                                auditId.toString()
                        )
                );

        return redisTemplate
                .opsForStream()
                .add(record)
                .getValue();
    }

    private boolean isGroupAlreadyExists(Exception ex) {

        Throwable cause = ex;

        while (cause != null) {

            if (cause.getMessage() != null
                    && cause.getMessage().contains("BUSYGROUP")) {

                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}