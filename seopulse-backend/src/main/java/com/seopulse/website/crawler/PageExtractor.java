package com.seopulse.website.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seopulse.website.entity.PageSignals;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Extracts the SEO-relevant facts from one HTML document. Kept separate
 * from the crawler so it can be tested against HTML fixtures.
 */
public class PageExtractor {

    static final int MAX_STORED_INTERNAL_LINKS = 500;
    static final int MAX_STORED_EXTERNAL_LINKS = 100;
    private static final int MAX_SAMPLES = 5;
    private static final int MAX_JSON_LD_TYPES = 20;
    private static final int MAX_HREFLANG = 50;

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String MIXED_CONTENT_SELECTOR =
            "img[src], script[src], iframe[src], source[src], video[src], audio[src], "
                    + "embed[src], object[data], link[rel=stylesheet][href]";

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
            Set<String> internalLinks,
            PageSignals signals
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
        Set<String> externalLinks = new LinkedHashSet<>();
        int internalLinks = 0;
        int externalLinkCount = 0;

        for (Element anchor : document.select("a[href]")) {

            String normalized = urlNormalizer.normalize(anchor.absUrl("href"));

            if (normalized == null) {
                continue;
            }

            if (inScope.test(normalized)) {
                internalLinks++;
                links.add(normalized);
            } else {
                externalLinkCount++;
                if (externalLinks.size() < MAX_STORED_EXTERNAL_LINKS) {
                    externalLinks.add(normalized);
                }
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
                externalLinkCount,
                links,
                extractSignals(document, links, externalLinks)
        );
    }

    private PageSignals extractSignals(Document document, Set<String> internalLinks, Set<String> externalLinks) {

        JsonLd jsonLd = extractJsonLd(document);
        MixedContent mixed = extractMixedContent(document);

        return new PageSignals(
                hasMetaContent(document, "meta[property=og:title]"),
                hasMetaContent(document, "meta[property=og:description]"),
                hasMetaContent(document, "meta[property=og:image]"),
                hasMetaContent(document, "meta[name=twitter:card]"),
                metaContent(document, "meta[name=viewport]"),
                extractMetaRobots(document),
                null,
                jsonLd.blocks(),
                jsonLd.invalid(),
                jsonLd.types().isEmpty() ? null : jsonLd.types(),
                mixed.count(),
                mixed.samples().isEmpty() ? null : mixed.samples(),
                extractHreflang(document),
                internalLinks.stream().limit(MAX_STORED_INTERNAL_LINKS).toList(),
                List.copyOf(externalLinks),
                null,
                null,
                null,
                null
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

    private static boolean hasMetaContent(Document document, String selector) {
        return metaContent(document, selector) != null;
    }

    private static String metaContent(Document document, String selector) {
        Element element = document.selectFirst(selector);
        if (element == null) {
            return null;
        }
        String value = element.attr("content").trim();
        return value.isEmpty() ? null : truncate(value, 500);
    }

    private static String extractMetaRobots(Document document) {
        List<String> values = new ArrayList<>();
        for (Element element : document.select("meta[name=robots], meta[name=googlebot]")) {
            String value = element.attr("content").trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values.isEmpty() ? null : truncate(String.join(", ", values), 500);
    }

    private record JsonLd(int blocks, int invalid, List<String> types) {
    }

    private static JsonLd extractJsonLd(Document document) {
        int blocks = 0;
        int invalid = 0;
        Set<String> types = new LinkedHashSet<>();

        for (Element script : document.select("script[type=application/ld+json]")) {
            blocks++;
            try {
                JsonNode root = JSON.readTree(script.data());
                if (root == null || !collectTypes(root, types)) {
                    invalid++;
                }
            } catch (IOException ex) {
                invalid++;
            }
        }

        return new JsonLd(blocks, invalid, types.stream().limit(MAX_JSON_LD_TYPES).toList());
    }

    /** Returns false when a block declares no {@code @type} anywhere. */
    private static boolean collectTypes(JsonNode node, Set<String> types) {
        if (node.isArray()) {
            boolean any = false;
            for (JsonNode item : node) {
                any |= collectTypes(item, types);
            }
            return any;
        }
        if (!node.isObject()) {
            return false;
        }
        boolean found = false;
        JsonNode type = node.get("@type");
        if (type != null) {
            if (type.isArray()) {
                type.forEach(value -> types.add(value.asText()));
                found = !type.isEmpty();
            } else if (type.isTextual() && !type.asText().isBlank()) {
                types.add(type.asText());
                found = true;
            }
        }
        JsonNode graph = node.get("@graph");
        if (graph != null) {
            found |= collectTypes(graph, types);
        }
        return found;
    }

    private record MixedContent(Integer count, List<String> samples) {
    }

    private static MixedContent extractMixedContent(Document document) {
        String location = document.location();
        if (location == null || !location.toLowerCase(Locale.ROOT).startsWith("https://")) {
            return new MixedContent(null, List.of());
        }

        int count = 0;
        List<String> samples = new ArrayList<>();
        for (Element element : document.select(MIXED_CONTENT_SELECTOR)) {
            String attribute = element.hasAttr("src") ? "src" : element.hasAttr("data") ? "data" : "href";
            String url = element.absUrl(attribute);
            if (url.regionMatches(true, 0, "http://", 0, 7)) {
                count++;
                if (samples.size() < MAX_SAMPLES) {
                    samples.add(truncate(url, 500));
                }
            }
        }
        return new MixedContent(count, samples);
    }

    private List<PageSignals.Hreflang> extractHreflang(Document document) {
        List<PageSignals.Hreflang> result = new ArrayList<>();
        for (Element link : document.select("link[rel=alternate][hreflang]")) {
            if (result.size() >= MAX_HREFLANG) {
                break;
            }
            String href = urlNormalizer.normalize(link.absUrl("href"));
            result.add(new PageSignals.Hreflang(truncate(link.attr("hreflang").trim(), 40), href));
        }
        return result.isEmpty() ? null : result;
    }

    private static String truncate(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }
}
