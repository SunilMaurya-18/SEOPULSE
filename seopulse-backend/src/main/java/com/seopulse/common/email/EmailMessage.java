package com.seopulse.common.email;

public record EmailMessage(
        String to,
        String subject,
        String body,
        String html
) {
    public EmailMessage(String to, String subject, String body) {
        this(to, subject, body, null);
    }
}
