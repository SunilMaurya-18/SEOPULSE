package com.seopulse.website.comparison;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @param newFingerprints fingerprints of every new issue (capped), so the
 *                        issue list can badge them without another request
 */
public record ComparisonResponse(
        Long auditId,
        Long baselineAuditId,
        Instant baselineCompletedAt,
        Integer score,
        Integer baselineScore,
        Integer scoreDelta,
        Integer pagesCrawled,
        Integer baselinePagesCrawled,
        Integer pagesDelta,
        boolean detailsAvailable,
        int newCount,
        int fixedCount,
        int persistingCount,
        Map<String, Integer> newBySeverity,
        Map<String, Integer> fixedBySeverity,
        List<IssueChange> newIssues,
        List<IssueChange> fixedIssues,
        List<String> newFingerprints
) {

    public record IssueChange(
            String fingerprint,
            String ruleCode,
            String ruleTitle,
            String severity,
            String category,
            String url,
            String message
    ) {
    }
}
