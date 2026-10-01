package com.seopulse.report.render;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Everything a report shows, used by both the PDF and the shared web page.
 *
 * @param branding null renders the standard SEOPulse look
 */
public record ReportModel(
        Long auditId,
        String websiteName,
        String websiteUrl,
        Instant completedAt,
        Integer score,
        Integer scoreVersion,
        Map<String, Integer> categoryScores,
        int pagesCrawled,
        int issueCount,
        int errorCount,
        int warningCount,
        int infoCount,
        Integer previousScore,
        Integer newIssues,
        Integer fixedIssues,
        boolean detailsAvailable,
        List<RuleGroup> topIssues,
        Branding branding,
        boolean watermark
) {

    public record RuleGroup(
            String ruleCode,
            String title,
            String severity,
            String category,
            int count,
            String recommendation,
            String helpUrl,
            List<String> sampleUrls
    ) {
    }

    public record Branding(
            String companyName,
            String brandColor,
            String coverText,
            String logoDataUrl
    ) {
    }
}
