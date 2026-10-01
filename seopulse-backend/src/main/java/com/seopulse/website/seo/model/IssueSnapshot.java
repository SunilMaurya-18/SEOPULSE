package com.seopulse.website.seo.model;

public record IssueSnapshot(
        String fingerprint,
        String ruleCode,
        String severity,
        String category,
        String url,
        String message
) {
}
