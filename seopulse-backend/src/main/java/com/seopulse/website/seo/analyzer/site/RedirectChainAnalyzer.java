package com.seopulse.website.seo.analyzer.site;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditPageStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Redirects that take more than one hop, and redirect loops. Uses the
 * chains the crawler recorded for each redirecting URL.
 */
@Component
public class RedirectChainAnalyzer implements SiteAnalyzer {

    @Override
    public String getName() {
        return "Redirect Chain Analyzer";
    }

    @Override
    public List<SiteIssue> analyze(SiteContext context) {

        List<SiteIssue> issues = new ArrayList<>();

        for (AuditPage page : context.pages()) {
            if (page.getStatus() != AuditPageStatus.REDIRECT) {
                continue;
            }

            List<String> chain = page.getRedirectChain() == null ? List.of() : page.getRedirectChain();

            if ("Redirect loop".equals(page.getSkipReason())) {
                issues.add(SiteIssue.of(page, "REDIRECT_LOOP", "ERROR",
                        "Redirects loop back to an earlier URL after " + Math.max(1, chain.size() - 1) + " hops"));
            } else if (chain.size() > 2) {
                issues.add(SiteIssue.of(page, "REDIRECT_CHAIN", "WARNING",
                        "Takes " + (chain.size() - 1) + " redirects to reach " + chain.getLast()));
            }
        }

        return issues;
    }
}
