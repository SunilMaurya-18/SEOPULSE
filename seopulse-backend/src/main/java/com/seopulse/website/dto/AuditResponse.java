package com.seopulse.website.dto;

import com.seopulse.website.entity.AuditStatus;

import java.time.Instant;

public record AuditResponse(

        Long id,

        Long websiteId,

        String websiteUrl,

        AuditStatus status,

        Integer score,

        Integer pagesCrawled,

        Integer pagesAnalyzed,

        Instant startedAt,

        Instant completedAt,

        String errorMessage,

        Instant createdAt

) {
}