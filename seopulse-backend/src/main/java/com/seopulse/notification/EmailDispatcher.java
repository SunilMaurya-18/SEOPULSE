package com.seopulse.notification;

import com.seopulse.common.email.EmailMessage;
import com.seopulse.common.email.EmailSender;
import com.seopulse.notification.entity.EmailOutbox;
import com.seopulse.notification.repository.EmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailDispatcher {

    private static final int BATCH_SIZE = 20;

    private final EmailOutboxRepository emailOutboxRepository;
    private final EmailSender emailSender;

    @Transactional
    public int dispatchPending() {
        List<EmailOutbox> pending = emailOutboxRepository.lockDispatchable(BATCH_SIZE);
        int sent = 0;
        for (EmailOutbox message : pending) {
            try {
                emailSender.send(new EmailMessage(
                        message.getToAddress(),
                        message.getSubject(),
                        message.getBodyText(),
                        message.getBodyHtml()
                ));
                message.setSent(true);
                message.setSentAt(Instant.now());
                message.setLastError(null);
                sent++;
            } catch (RuntimeException ex) {
                message.setAttempts(message.getAttempts() + 1);
                String detail = ex.getMessage() == null ? "send failed" : ex.getMessage();
                message.setLastError(detail.length() > 500 ? detail.substring(0, 500) : detail);
                log.warn("Email delivery failed: id={}, attempts={}, error={}",
                        message.getId(), message.getAttempts(), message.getLastError());
            }
        }
        return sent;
    }
}
