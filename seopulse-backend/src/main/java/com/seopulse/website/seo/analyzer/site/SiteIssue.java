package com.seopulse.website.seo.analyzer.site;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.model.SeoIssueResult;

public record SiteIssue(AuditPage page, SeoIssueResult result) {

    public static SiteIssue of(AuditPage page, String ruleCode, String severity, String message) {
        String trimmed = message.length() > 500 ? message.substring(0, 497) + "..." : message;
        return new SiteIssue(page, new SeoIssueResult(ruleCode, severity, trimmed));
    }
}
