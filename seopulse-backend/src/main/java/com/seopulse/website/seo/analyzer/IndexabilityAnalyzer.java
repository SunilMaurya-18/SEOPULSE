package com.seopulse.website.seo.analyzer;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.PageSignals;
import com.seopulse.website.seo.model.SeoIssueResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class IndexabilityAnalyzer implements SeoAnalyzer {

    @Override
    public String getName() {
        return "Indexability Analyzer";
    }

    @Override
    public List<SeoIssueResult> analyze(AuditPage page) {

        if (!OpenGraphAnalyzer.isSuccess(page)) {
            return List.of();
        }

        List<SeoIssueResult> issues = new ArrayList<>();
        PageSignals signals = page.getSignals();

        if (signals != null) {
            if (blocksIndexing(signals.metaRobots())) {
                issues.add(new SeoIssueResult("NOINDEX", "WARNING", "Meta robots tag contains noindex"));
            } else if (blocksIndexing(signals.xRobotsTag())) {
                issues.add(new SeoIssueResult("NOINDEX", "WARNING", "X-Robots-Tag header contains noindex"));
            }
        }

        String canonical = page.getCanonicalUrl();
        if (canonical != null && !sameUrl(canonical, page.getUrl())) {
            issues.add(new SeoIssueResult(
                    "CANONICAL_POINTS_ELSEWHERE",
                    "INFO",
                    truncate("Canonical URL points to " + canonical)
            ));
        }

        return issues;
    }

    static boolean blocksIndexing(String directives) {
        if (directives == null) {
            return false;
        }
        for (String part : directives.toLowerCase(Locale.ROOT).split("[,\\s]+")) {
            // X-Robots-Tag may be scoped to a user agent ("googlebot: noindex").
            String directive = part.contains(":") ? part.substring(part.indexOf(':') + 1) : part;
            if (directive.equals("noindex") || directive.equals("none")) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameUrl(String a, String b) {
        return b != null && strip(a).equalsIgnoreCase(strip(b));
    }

    private static String strip(String url) {
        return url.replaceFirst("/+$", "");
    }

    private static String truncate(String message) {
        return message.length() > 500 ? message.substring(0, 497) + "..." : message;
    }
}
