package com.seopulse.website.seo.model;

public record SeoIssueResult(
        String ruleCode,
        String severity,
        String message
) {
}