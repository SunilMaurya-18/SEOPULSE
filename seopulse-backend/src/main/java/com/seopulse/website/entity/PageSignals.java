package com.seopulse.website.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Raw on-page facts kept for analyzers that need more than the fixed
 * {@code audit_pages} columns. Stored as JSON, so new signals do not need
 * a migration; every field is optional.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageSignals(
        Boolean ogTitle,
        Boolean ogDescription,
        Boolean ogImage,
        Boolean twitterCard,
        String viewport,
        String metaRobots,
        String xRobotsTag,
        Integer jsonLdBlocks,
        Integer jsonLdInvalid,
        List<String> jsonLdTypes,
        Integer mixedContent,
        List<String> mixedContentSamples,
        List<Hreflang> hreflang,
        List<String> internalLinks,
        List<String> externalLinks,
        Boolean inSitemap,
        Integer htmlBytes,
        String hsts,
        Boolean httpsEnforced
) {

    public record Hreflang(String lang, String href) {
    }

    public static PageSignals empty() {
        return new PageSignals(null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    public PageSignals withResponse(String xRobotsTag, Integer htmlBytes, String hsts) {
        return new PageSignals(ogTitle, ogDescription, ogImage, twitterCard, viewport, metaRobots, xRobotsTag,
                jsonLdBlocks, jsonLdInvalid, jsonLdTypes, mixedContent, mixedContentSamples, hreflang,
                internalLinks, externalLinks, inSitemap, htmlBytes, hsts, httpsEnforced);
    }

    public PageSignals withInSitemap(Boolean inSitemap) {
        return new PageSignals(ogTitle, ogDescription, ogImage, twitterCard, viewport, metaRobots, xRobotsTag,
                jsonLdBlocks, jsonLdInvalid, jsonLdTypes, mixedContent, mixedContentSamples, hreflang,
                internalLinks, externalLinks, inSitemap, htmlBytes, hsts, httpsEnforced);
    }

    public PageSignals withHttpsEnforced(Boolean httpsEnforced) {
        return new PageSignals(ogTitle, ogDescription, ogImage, twitterCard, viewport, metaRobots, xRobotsTag,
                jsonLdBlocks, jsonLdInvalid, jsonLdTypes, mixedContent, mixedContentSamples, hreflang,
                internalLinks, externalLinks, inSitemap, htmlBytes, hsts, httpsEnforced);
    }

    public List<String> internalLinksOrEmpty() {
        return internalLinks == null ? List.of() : internalLinks;
    }

    public List<String> externalLinksOrEmpty() {
        return externalLinks == null ? List.of() : externalLinks;
    }

    public List<Hreflang> hreflangOrEmpty() {
        return hreflang == null ? List.of() : hreflang;
    }
}
