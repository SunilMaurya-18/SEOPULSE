package com.seopulse.website.seo.model;

public record IssueRow(
        Long pageId,
        String ruleCode,
        String severity,
        String category
) {
}
