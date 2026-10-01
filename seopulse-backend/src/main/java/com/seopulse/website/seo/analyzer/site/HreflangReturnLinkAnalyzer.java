package com.seopulse.website.seo.analyzer.site;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.PageSignals;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Every hreflang alternate must link back. Only alternates that were
 * crawled in the same audit can be checked.
 */
@Component
public class HreflangReturnLinkAnalyzer implements SiteAnalyzer {

    @Override
    public String getName() {
        return "Hreflang Return Link Analyzer";
    }

    @Override
    public List<SiteIssue> analyze(SiteContext context) {

        Map<String, AuditPage> byUrl = context.pagesByUrl();
        List<SiteIssue> issues = new ArrayList<>();

        for (AuditPage page : context.successfulPages()) {
            if (page.getSignals() == null) {
                continue;
            }

            List<String> missing = page.getSignals().hreflangOrEmpty().stream()
                    .map(PageSignals.Hreflang::href)
                    .filter(Objects::nonNull)
                    .filter(href -> !href.equals(page.getUrl()))
                    .distinct()
                    .filter(href -> {
                        AuditPage alternate = byUrl.get(href);
                        return alternate != null
                                && alternate.getSignals() != null
                                && alternate.getStatusCode() != null
                                && alternate.getStatusCode() < 300
                                && alternate.getSignals().hreflangOrEmpty().stream()
                                .noneMatch(entry -> page.getUrl().equals(entry.href()));
                    })
                    .toList();

            if (!missing.isEmpty()) {
                issues.add(SiteIssue.of(page, "HREFLANG_MISSING_RETURN", "WARNING",
                        missing.size() + (missing.size() == 1 ? " alternate does" : " alternates do")
                                + " not link back, e.g. " + missing.getFirst()));
            }
        }

        return issues;
    }
}
