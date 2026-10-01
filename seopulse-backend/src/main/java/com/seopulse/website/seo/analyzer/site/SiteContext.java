package com.seopulse.website.seo.analyzer.site;

import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.entity.AuditPageStatus;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every page of one audit, indexed for site-level analysis.
 *
 * @param externalLinkStatus HTTP status per checked external URL; -1 when the host no longer resolves
 */
public record SiteContext(
        List<AuditPage> pages,
        Map<String, Integer> externalLinkStatus
) {

    public List<AuditPage> crawledPages() {
        return pages.stream()
                .filter(page -> page.getStatus() == AuditPageStatus.CRAWLED)
                .toList();
    }

    /** Crawled pages that returned 2xx. */
    public List<AuditPage> successfulPages() {
        return crawledPages().stream()
                .filter(page -> page.getStatusCode() != null && page.getStatusCode() >= 200 && page.getStatusCode() < 300)
                .toList();
    }

    public Map<String, AuditPage> pagesByUrl() {
        Map<String, AuditPage> byUrl = new LinkedHashMap<>();
        for (AuditPage page : pages) {
            byUrl.putIfAbsent(page.getUrl(), page);
        }
        return byUrl;
    }

    public Optional<AuditPage> homepage() {
        return crawledPages().stream()
                .filter(page -> page.getDepth() != null && page.getDepth() == 0)
                .min(Comparator.comparing(AuditPage::getId, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /** Number of other crawled pages linking to each URL. */
    public Map<String, Integer> inboundLinkCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (AuditPage page : crawledPages()) {
            if (page.getSignals() == null) {
                continue;
            }
            page.getSignals().internalLinksOrEmpty().stream()
                    .distinct()
                    .filter(link -> !link.equals(page.getUrl()))
                    .forEach(link -> counts.merge(link, 1, Integer::sum));
        }
        return counts;
    }
}
