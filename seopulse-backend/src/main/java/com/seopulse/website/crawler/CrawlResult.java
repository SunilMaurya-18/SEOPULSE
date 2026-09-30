package com.seopulse.website.crawler;

import java.util.List;

/**
 * @param timedOut true when the crawl stopped because the time budget ran out
 */
public record CrawlResult(
        List<CrawledPage> pages,
        boolean timedOut
) {
}
