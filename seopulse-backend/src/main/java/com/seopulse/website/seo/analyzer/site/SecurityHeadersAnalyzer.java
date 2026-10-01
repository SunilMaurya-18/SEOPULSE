package com.seopulse.website.seo.analyzer.site;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.PageSignals;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Site-wide transport security, reported once on the homepage.
 */
@Component
public class SecurityHeadersAnalyzer implements SiteAnalyzer {

    @Override
    public String getName() {
        return "Security Headers Analyzer";
    }

    @Override
    public List<SiteIssue> analyze(SiteContext context) {

        AuditPage homepage = context.homepage().orElse(null);

        if (homepage == null || homepage.getSignals() == null) {
            return List.of();
        }

        PageSignals signals = homepage.getSignals();
        List<SiteIssue> issues = new ArrayList<>();
        boolean https = homepage.getUrl().toLowerCase(Locale.ROOT).startsWith("https://");

        if (Boolean.FALSE.equals(signals.httpsEnforced())) {
            issues.add(SiteIssue.of(homepage, "HTTPS_NOT_ENFORCED", "ERROR", https
                    ? "The http:// version of the site does not redirect to https://"
                    : "The site is served over plain HTTP"));
        }

        if (https && (signals.hsts() == null || !signals.hsts().toLowerCase(Locale.ROOT).contains("max-age"))) {
            issues.add(SiteIssue.of(homepage, "HSTS_MISSING", "WARNING",
                    "The homepage does not send a Strict-Transport-Security header"));
        }

        return issues;
    }
}
