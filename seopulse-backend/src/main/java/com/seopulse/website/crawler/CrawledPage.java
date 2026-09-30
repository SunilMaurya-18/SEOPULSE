package com.seopulse.website.crawler;

import java.util.List;
import java.util.Set;

public record CrawledPage(
        String url,
        Outcome outcome,
        int status,
        String contentType,
        String title,
        String metaDescription,
        String canonicalUrl,
        int wordCount,
        int h1Count,
        int imageCount,
        int imagesWithoutAlt,
        int internalLinkCount,
        int externalLinkCount,
        int depth,
        Set<String> discoveredUrls,
        String finalUrl,
        List<String> redirectChain,
        String skipReason
) {

    public enum Outcome {
        /** Fetched; {@code status} may still be a 4xx/5xx error. */
        CRAWLED,
        /** The URL redirected; see {@code finalUrl} and {@code redirectChain}. */
        REDIRECT,
        /** Not fetched because robots.txt disallows it. */
        SKIPPED_ROBOTS,
        /** Response body exceeded the configured size limit. */
        TOO_LARGE,
        /** Network error, timeout or blocked address. */
        FAILED
    }

    static CrawledPage withoutContent(
            String url,
            Outcome outcome,
            int status,
            String contentType,
            int depth,
            String finalUrl,
            List<String> redirectChain,
            String skipReason
    ) {
        return new CrawledPage(
                url, outcome, status, contentType,
                null, null, null,
                0, 0, 0, 0, 0, 0,
                depth, Set.of(),
                finalUrl, redirectChain, skipReason
        );
    }
}
