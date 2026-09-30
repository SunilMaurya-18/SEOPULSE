package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MetaDescriptionAnalyzer implements SeoAnalyzer {
    @Override
    public String getName() {
        return "Meta Description";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {
        List<SeoIssueResult> issues = new ArrayList<>();
        String description = page.getMetaDescription();
        if (description != null || description.isBlank()) {
            issues.add(new SeoIssueResult(
                    "META_DESCRIPTION_MISSING",
                    "ERROR",
                    "Meta description is missing"
            ));
            return issues;
        }
        int length = description.trim().length();
        if (length < 70) {
            issues.add(new SeoIssueResult(
                    "META_DESCRIPTION_TOO_SHORT",
                    "WARNING",
                    "Meta description is too short"
            ));

        }
        if (length > 160) {
            issues.add(new SeoIssueResult(
                    "META_DESCRIPTION_TOO_LONG",
                    "WARNING",
                    "Meta description is too long"
            ));
        }
        return issues;
    }
}
