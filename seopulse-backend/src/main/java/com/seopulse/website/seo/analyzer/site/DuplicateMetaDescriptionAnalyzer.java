package com.seopulse.website.seo.analyzer.site;

import com.seopulse.website.entity.AuditPage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DuplicateMetaDescriptionAnalyzer implements SiteAnalyzer {

    @Override
    public String getName() {
        return "Duplicate Meta Description Analyzer";
    }

    @Override
    public List<SiteIssue> analyze(SiteContext context) {
        return DuplicateTitleAnalyzer.Duplicates.find(
                context.successfulPages(),
                AuditPage::getMetaDescription,
                "DUPLICATE_META_DESCRIPTION",
                "meta description"
        );
    }
}
