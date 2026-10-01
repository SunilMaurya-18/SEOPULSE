package com.seopulse.website.comparison;

import com.seopulse.website.entity.Audit;
import com.seopulse.website.seo.model.IssueSnapshot;

import java.util.List;
import java.util.Map;

/**
 * Differences between an audit and its baseline (normally the previous
 * completed audit of the same website).
 *
 * @param baseline         null when there is nothing to compare with
 * @param detailsAvailable false when either audit's issues were removed by retention
 */
public record AuditComparison(
        Audit target,
        Audit baseline,
        boolean detailsAvailable,
        List<IssueSnapshot> newIssues,
        List<IssueSnapshot> fixedIssues,
        int persistingCount,
        Map<String, Integer> newBySeverity,
        Map<String, Integer> fixedBySeverity
) {

    public Integer scoreDelta() {
        if (baseline == null || baseline.getScore() == null || target.getScore() == null) {
            return null;
        }
        return target.getScore() - baseline.getScore();
    }

    public int newErrorCount() {
        return newBySeverity.getOrDefault("ERROR", 0);
    }
}
