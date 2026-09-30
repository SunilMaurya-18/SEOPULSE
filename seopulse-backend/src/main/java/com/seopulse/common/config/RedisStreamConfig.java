package com.seopulse.common.config;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisStreamConfig {
    public static final String AUDIT_STREAM = "seopulse:audit:stream";
    public static final String AUDIT_CONSUMER_GROUP = "seopulse:audit:workers";
    public static final String AUDIT_CONSUMER_NAME = "seopulse:audit:worker-1";

    public final RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void initializeConsumerGroup() {
        StreamOperations<String, Object, Object> streamOperations =
                redisTemplate.opsForStream();
        try {
            streamOperations.createGroup(
                    AUDIT_STREAM,
                    ReadOffset.from("0-0"),
                    AUDIT_CONSUMER_GROUP
            );
        } catch (Exception e) {
            if (!isGroupAlreadyExistsException(e)) {
                throw e;
            }
        }
    }

    private boolean isGroupAlreadyExistsException(Exception e) {
        Throwable cause = e;

        while (cause != null) {

            if (cause.getMessage() != null &&
                    cause.getMessage()
                            .contains("BUSYGROUP")) {

                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}
