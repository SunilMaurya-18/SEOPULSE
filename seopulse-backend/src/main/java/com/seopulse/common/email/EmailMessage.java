package com.seopulse.common.email;

public record EmailMessage(
        String to,
        String subject,
        String body
) {
}
