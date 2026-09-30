package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ContentAnalyzer implements SeoAnalyzer {

    private static final int MIN_WORD_COUNT = 300;

    @Override
    public String getName() {
        return "Content Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {

        List<SeoIssueResult> issues =
                new ArrayList<>();

        if (page.getWordCount() < MIN_WORD_COUNT) {

            issues.add(new SeoIssueResult(
                    "LOW_WORD_COUNT",
                    "WARNING",
                    "Page contains very little textual content"
            ));
        }

        return issues;
    }
}