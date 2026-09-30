package com.seopulse.common.email;

/**
 * Outbound email. Phase 4 replaces the logging implementation with a real
 * provider.
 */
public interface EmailSender {

    void send(EmailMessage message);
}
