package com.seopulse.website.dto;

import java.time.Instant;

public record SeoIssueResponse(

        Long id,

        Long auditId,

        Long auditPageId,

        String url,

        String ruleCode,

        String severity,

        String message,

        String recommendation,

        Instant createdAt

) {
}