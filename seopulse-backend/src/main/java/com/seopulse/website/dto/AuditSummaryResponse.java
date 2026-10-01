package com.seopulse.website.dto;

import com.seopulse.website.entity.AuditStatus;

import java.time.Instant;
import java.util.Map;

public record AuditSummaryResponse(

        Long auditId,

        Long websiteId,

        String websiteUrl,

        AuditStatus status,

        Integer score,

        long pagesCrawled,

        long pagesAnalyzed,

        long totalIssues,

        long errorCount,

        long warningCount,

        long infoCount,

        Instant startedAt,

        Instant completedAt,

        Map<String, Integer> categoryScores,

        Integer scoreVersion,

        /* True once retention removed the page-level details; counts still apply. */
        boolean detailsPurged

) {}
