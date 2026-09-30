package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class H1Analyzer implements SeoAnalyzer {

    @Override
    public String getName() {
        return "H1 Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {

        List<SeoIssueResult> issues =
                new ArrayList<>();

        int h1Count = page.getH1Count();

        if (h1Count == 0) {

            issues.add(new SeoIssueResult(
                    "H1_MISSING",
                    "ERROR",
                    "Page does not contain an H1 heading"
            ));
        }

        if (h1Count > 1) {

            issues.add(new SeoIssueResult(
                    "MULTIPLE_H1",
                    "WARNING",
                    "Page contains multiple H1 headings"
            ));
        }

        return issues;
    }
}