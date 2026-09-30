package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LinkAnalyzer implements SeoAnalyzer {

    @Override
    public String getName() {
        return "Link Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {

        List<SeoIssueResult> issues =
                new ArrayList<>();

        if (page.getInternalLinkCount() == 0) {

            issues.add(new SeoIssueResult(
                    "NO_INTERNAL_LINKS",
                    "WARNING",
                    "Page contains no internal links"
            ));
        }

        return issues;
    }
}