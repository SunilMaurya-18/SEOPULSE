package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CanonicalAnalyzer implements SeoAnalyzer {
    @Override
    public String getName() {
        return "Canonical Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {
        List<SeoIssueResult> issues = new ArrayList<>();
        String canonical = page.getCanonicalUrl();
        if (canonical == null || canonical.isBlank()) {
            issues.add(new SeoIssueResult(
                    "CANONICAL_MISSING",
                    "WARNING",
                    "Page does not specify a canonical url"
            ));
        }
        return issues;
    }
}
