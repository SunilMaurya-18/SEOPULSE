package com.seopulse.website.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Extracts the SEO-relevant facts from one HTML document. Kept separate
 * from the crawler so it can be tested against HTML fixtures.
 */
public class PageExtractor {

    public record PageContent(
            String title,
            String metaDescription,
            String canonicalUrl,
            int wordCount,
            int h1Count,
            int imageCount,
            int imagesWithoutAlt,
            int internalLinkCount,
            int externalLinkCount,
            Set<String> internalLinks
    ) {
    }

    private final UrlNormalizer urlNormalizer;

    public PageExtractor(UrlNormalizer urlNormalizer) {
        this.urlNormalizer = urlNormalizer;
    }

    public static Document parse(byte[] body, String charset, String baseUrl) throws IOException {
        return Jsoup.parse(new ByteArrayInputStream(body), charset, baseUrl);
    }

    public PageContent extract(Document document, Predicate<String> inScope) {

        Set<String> links = new LinkedHashSet<>();
        int internalLinks = 0;
        int externalLinks = 0;

        for (Element anchor : document.select("a[href]")) {

            String normalized = urlNormalizer.normalize(anchor.absUrl("href"));

            if (normalized == null) {
                continue;
            }

            if (inScope.test(normalized)) {
                internalLinks++;
                links.add(normalized);
            } else {
                externalLinks++;
            }
        }

        return new PageContent(
                extractTitle(document),
                extractMetaDescription(document),
                extractCanonical(document),
                countWords(document),
                document.select("h1").size(),
                document.select("img").size(),
                countImagesWithoutAlt(document),
                internalLinks,
                externalLinks,
                links
        );
    }

    private static String extractTitle(Document document) {

        Element title = document.selectFirst("title");

        if (title == null) {
            return null;
        }

        String value = title.text().trim();

        return value.isBlank() ? null : value;
    }

    private static String extractMetaDescription(Document document) {

        Element element = document.selectFirst("meta[name=description]");

        if (element == null) {
            return null;
        }

        String value = element.attr("content").trim();

        return value.isBlank() ? null : value;
    }

    private String extractCanonical(Document document) {

        Element canonical = document.selectFirst("link[rel=canonical]");

        if (canonical == null) {
            return null;
        }

        return urlNormalizer.normalize(canonical.absUrl("href"));
    }

    private static int countWords(Document document) {

        String text = document.body() != null
                ? document.body().text()
                : document.text();

        if (text == null || text.isBlank()) {
            return 0;
        }

        return text.trim().split("\\s+").length;
    }

    /**
     * An empty alt attribute counts as missing for the current SEO rules.
     */
    private static int countImagesWithoutAlt(Document document) {

        int count = 0;

        for (Element image : document.select("img")) {
            if (image.attr("alt").isBlank()) {
                count++;
            }
        }

        return count;
    }
}
