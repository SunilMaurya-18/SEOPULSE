package com.seopulse.website.job;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "seopulse.worker")
@Getter
@Setter
public class WorkerProperties {
    private int pendingRecoveryIntervalMinutes = 5;
    private int pendingRecoveryLimit = 10;
    private int outboxPublishIntervalMs = 2000;
    private int outboxBatchSize = 50;
}
