package com.seopulse.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "seopulse.email.dispatch-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class EmailDispatchJob {

    private final EmailDispatcher emailDispatcher;

    @Scheduled(fixedDelayString = "${seopulse.email.dispatch-interval-ms:5000}")
    public void dispatch() {
        emailDispatcher.dispatchPending();
    }
}
