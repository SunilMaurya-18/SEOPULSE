package com.seopulse.website.crawler;

import java.util.List;
import java.util.Set;

/**
 * @param timedOut      true when the crawl stopped because the time budget ran out
 * @param sitemapUrls   page URLs that were listed in the site's sitemaps
 * @param httpsEnforced whether http:// requests for the site redirect to https://;
 *                      null when it could not be determined
 */
public record CrawlResult(
        List<CrawledPage> pages,
        boolean timedOut,
        Set<String> sitemapUrls,
        Boolean httpsEnforced
) {

    public CrawlResult(List<CrawledPage> pages, boolean timedOut) {
        this(pages, timedOut, Set.of(), null);
    }
}
