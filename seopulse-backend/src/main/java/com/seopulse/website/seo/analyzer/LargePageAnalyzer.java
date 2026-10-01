package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.PageSignals;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LargePageAnalyzer implements SeoAnalyzer {

    static final int MAX_HTML_BYTES = 512 * 1024;

    @Override
    public String getName() {
        return "Page Size Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {

        PageSignals signals = page.getSignals();

        if (signals == null || signals.htmlBytes() == null || signals.htmlBytes() <= MAX_HTML_BYTES) {
            return List.of();
        }

        return List.of(new SeoIssueResult(
                "LARGE_PAGE",
                "WARNING",
                "HTML document is " + (signals.htmlBytes() / 1024) + " KB (over " + (MAX_HTML_BYTES / 1024) + " KB)"
        ));
    }
}
