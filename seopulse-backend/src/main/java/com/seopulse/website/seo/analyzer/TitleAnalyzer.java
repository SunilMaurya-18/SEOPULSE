package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TitleAnalyzer implements SeoAnalyzer {

    @Override
    public String getName() {
        return "Title Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {

        List<SeoIssueResult> issues =
                new ArrayList<>();

        String title = page.getTitle();

        if (title == null || title.isBlank()) {

            issues.add(
                    new SeoIssueResult(
                            "TITLE_MISSING",
                            "ERROR",
                            "Page title is missing"
                    )
            );

            return issues;
        }

        int length = title.trim().length();

        if (length < 30) {

            issues.add(
                    new SeoIssueResult(
                            "TITLE_TOO_SHORT",
                            "WARNING",
                            "Page title is too short"
                    )
            );
        }

        if (length > 60) {

            issues.add(
                    new SeoIssueResult(
                            "TITLE_TOO_LONG",
                            "WARNING",
                            "Page title is too long"
                    )
            );
        }

        return issues;
    }
}