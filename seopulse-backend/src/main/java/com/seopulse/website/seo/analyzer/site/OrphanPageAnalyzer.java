package com.seopulse.website.seo.analyzer.site;

import com.seopulse.website.entity.AuditPage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Pages listed in the sitemap that no crawled page links to.
 */
@Component
public class OrphanPageAnalyzer implements SiteAnalyzer {

    @Override
    public String getName() {
        return "Orphan Page Analyzer";
    }

    @Override
    public List<SiteIssue> analyze(SiteContext context) {

        Map<String, Integer> inbound = context.inboundLinkCounts();
        Long homepageId = context.homepage().map(AuditPage::getId).orElse(null);

        return context.successfulPages().stream()
                .filter(page -> page.getSignals() != null && Boolean.TRUE.equals(page.getSignals().inSitemap()))
                .filter(page -> homepageId == null || !homepageId.equals(page.getId()))
                .filter(page -> inbound.getOrDefault(page.getUrl(), 0) == 0)
                .map(page -> SiteIssue.of(page, "ORPHAN_PAGE", "WARNING",
                        "Listed in the sitemap but no crawled page links to it"))
                .toList();
    }
}
