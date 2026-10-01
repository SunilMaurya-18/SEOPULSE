package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.PageSignals;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OpenGraphAnalyzer implements SeoAnalyzer {

    @Override
    public String getName() {
        return "Open Graph Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {

        PageSignals signals = page.getSignals();

        if (signals == null || !isSuccess(page)) {
            return List.of();
        }

        List<SeoIssueResult> issues = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        if (!Boolean.TRUE.equals(signals.ogTitle())) {
            missing.add("og:title");
        }
        if (!Boolean.TRUE.equals(signals.ogDescription())) {
            missing.add("og:description");
        }
        if (!Boolean.TRUE.equals(signals.ogImage())) {
            missing.add("og:image");
        }

        if (!missing.isEmpty()) {
            issues.add(new SeoIssueResult(
                    "OPEN_GRAPH_INCOMPLETE",
                    "WARNING",
                    "Missing Open Graph tags: " + String.join(", ", missing)
            ));
        }

        if (!Boolean.TRUE.equals(signals.twitterCard())) {
            issues.add(new SeoIssueResult(
                    "TWITTER_CARD_MISSING",
                    "INFO",
                    "Page has no twitter:card meta tag"
            ));
        }

        return issues;
    }

    static boolean isSuccess(AuditPage page) {
        Integer status = page.getStatusCode();
        return status != null && status >= 200 && status < 300;
    }
}
