package com.seopulse.website.comparison;

import com.seopulse.website.entity.Audit;
import com.seopulse.website.entity.AuditTrigger;

import java.time.Instant;
import java.util.Map;

public record TrendPoint(
        Long auditId,
        Instant completedAt,
        Integer score,
        Integer scoreVersion,
        Integer issueCount,
        Integer errorCount,
        Integer warningCount,
        Integer infoCount,
        Integer pagesCrawled,
        Map<String, Integer> categoryScores,
        AuditTrigger triggeredBy
) {

    static TrendPoint from(Audit audit) {
        return new TrendPoint(
                audit.getId(),
                audit.getCompletedAt(),
                audit.getScore(),
                audit.getScoreVersion(),
                audit.getIssueCount(),
                audit.getErrorCount(),
                audit.getWarningCount(),
                audit.getInfoCount(),
                audit.getPagesCrawled(),
                audit.getCategoryScores(),
                audit.getTriggeredBy()
        );
    }
}
