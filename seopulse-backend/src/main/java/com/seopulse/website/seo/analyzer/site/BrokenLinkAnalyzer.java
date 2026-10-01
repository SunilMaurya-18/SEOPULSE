package com.seopulse.website.seo.analyzer.site;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditPageStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Internal links to crawled pages that returned 4xx/5xx, and external links
 * that the {@link ExternalLinkChecker} found broken.
 */
@Component
public class BrokenLinkAnalyzer implements SiteAnalyzer {

    @Override
    public String getName() {
        return "Broken Link Analyzer";
    }

    @Override
    public List<SiteIssue> analyze(SiteContext context) {

        Map<String, AuditPage> byUrl = context.pagesByUrl();
        Map<String, Integer> external = context.externalLinkStatus();
        List<SiteIssue> issues = new ArrayList<>();

        for (AuditPage page : context.crawledPages()) {
            if (page.getSignals() == null) {
                continue;
            }

            List<String> brokenInternal = page.getSignals().internalLinksOrEmpty().stream()
                    .distinct()
                    .filter(link -> isBroken(byUrl.get(link), byUrl))
                    .toList();

            if (!brokenInternal.isEmpty()) {
                issues.add(SiteIssue.of(
                        page,
                        "BROKEN_INTERNAL_LINK",
                        "ERROR",
                        describe(brokenInternal.size(), "broken internal page") + ", e.g. " + brokenInternal.getFirst()
                ));
            }

            List<String> brokenExternal = page.getSignals().externalLinksOrEmpty().stream()
                    .distinct()
                    .filter(link -> ExternalLinkChecker.isBroken(external.get(link)))
                    .toList();

            if (!brokenExternal.isEmpty()) {
                issues.add(SiteIssue.of(
                        page,
                        "BROKEN_EXTERNAL_LINK",
                        "WARNING",
                        describe(brokenExternal.size(), "broken external page") + ", e.g. " + brokenExternal.getFirst()
                ));
            }
        }

        return issues;
    }

    private static boolean isBroken(AuditPage target, Map<String, AuditPage> byUrl) {
        if (target == null) {
            return false;
        }
        if (target.getStatus() == AuditPageStatus.REDIRECT && target.getFinalUrl() != null) {
            AuditPage finalPage = byUrl.get(target.getFinalUrl());
            return finalPage != null && finalPage != target && isErrorStatus(finalPage);
        }
        return isErrorStatus(target);
    }

    private static boolean isErrorStatus(AuditPage page) {
        return page.getStatus() == AuditPageStatus.CRAWLED
                && page.getStatusCode() != null
                && page.getStatusCode() >= 400;
    }

    private static String describe(int count, String noun) {
        return "Links to " + count + " " + noun + (count == 1 ? "" : "s");
    }
}
