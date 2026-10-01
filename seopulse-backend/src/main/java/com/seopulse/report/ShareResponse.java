package com.seopulse.report;

import java.time.Instant;

/**
 * @param url the share URL; only returned when the link is created, because
 *            the token is not stored
 */
public record ShareResponse(
        Long id,
        Long auditId,
        String url,
        Instant expiresAt,
        Instant revokedAt,
        boolean active,
        int viewCount,
        Instant lastViewedAt,
        Instant createdAt
) {

    static ShareResponse from(ReportShare share, String url) {
        return new ShareResponse(
                share.getId(),
                share.getAuditId(),
                url,
                share.getExpiresAt(),
                share.getRevokedAt(),
                share.isActive(Instant.now()),
                share.getViewCount(),
                share.getLastViewedAt(),
                share.getCreatedAt()
        );
    }
}
