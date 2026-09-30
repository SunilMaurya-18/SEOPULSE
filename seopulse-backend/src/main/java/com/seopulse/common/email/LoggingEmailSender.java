package com.seopulse.common.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Development stand-in for a real email provider. Message bodies contain
 * single-use links, so they are only logged when explicitly enabled
 * (never in prod).
 */
@Component
@Slf4j
public class LoggingEmailSender implements EmailSender {

    private final boolean logContent;

    public LoggingEmailSender(@Value("${seopulse.email.log-content:false}") boolean logContent) {
        this.logContent = logContent;
    }

    @Override
    public void send(EmailMessage message) {
        if (logContent) {
            log.info("Email (not sent): to={}, subject={}\n{}", message.to(), message.subject(), message.body());
        } else {
            log.info("Email (not sent): subject={}", message.subject());
        }
    }
}
