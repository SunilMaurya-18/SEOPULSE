package com.seopulse.website.crawler;

import com.seopulse.website.crawler.net.CrawlerHttpClient;
import com.seopulse.website.crawler.net.CrawlerHttpClient.FetchGate;
import com.seopulse.website.crawler.net.FetchResponse;
import com.seopulse.website.service.UrlValidator;
import crawlercommons.sitemaps.AbstractSiteMap;
import crawlercommons.sitemaps.SiteMap;
import crawlercommons.sitemaps.SiteMapIndex;
import crawlercommons.sitemaps.SiteMapParser;
import crawlercommons.sitemaps.SiteMapURL;
import crawlercommons.sitemaps.UnknownFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Discovers page URLs from XML sitemaps (including sitemap indexes and
 * gzipped sitemaps). Sitemaps come from robots.txt, or /sitemap.xml when
 * robots.txt lists none.
 */
@Service
@Slf4j
public class SitemapService {

    private static final String ACCEPT = "application/xml,text/xml,application/gzip,*/*;q=0.5";

    private final CrawlerProperties properties;
    private final CrawlerHttpClient httpClient;
    private final UrlValidator urlValidator;
    private final UrlNormalizer urlNormalizer;

    public SitemapService(
            CrawlerProperties properties,
            CrawlerHttpClient httpClient,
            UrlValidator urlValidator,
            UrlNormalizer urlNormalizer
    ) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.urlValidator = urlValidator;
        this.urlNormalizer = urlNormalizer;
    }

    /**
     * @param origin          site origin, used for the /sitemap.xml fallback
     * @param robotsSitemaps  sitemap URLs declared in robots.txt
     * @param inScope         filter for page URLs that belong to the site
     * @param limit           maximum number of page URLs to return
     */
    public List<String> discover(
            String origin,
            List<String> robotsSitemaps,
            Predicate<String> inScope,
            int limit,
            FetchGate gate
    ) throws InterruptedException {

        Deque<String> queue = new ArrayDeque<>(
                robotsSitemaps.isEmpty() ? List.of(origin + "/sitemap.xml") : robotsSitemaps
        );
        Set<String> seenSitemaps = new HashSet<>(queue);
        Set<String> pageUrls = new LinkedHashSet<>();
        SiteMapParser parser = new SiteMapParser(false);

        int fetched = 0;

        while (!queue.isEmpty()
                && fetched < properties.getMaxSitemaps()
                && pageUrls.size() < limit) {

            String sitemapUrl = queue.poll();
            fetched++;

            try {
                URI uri = urlValidator.validateStructure(sitemapUrl);

                FetchResponse response = httpClient.fetchFollowingRedirects(
                        uri,
                        ACCEPT,
                        properties.getMaxSitemapSizeBytes(),
                        properties.getMaxRedirects(),
                        gate
                );

                if (!response.isSuccess() || response.tooLarge() || response.body().length == 0) {
                    continue;
                }

                AbstractSiteMap sitemap = parser.parseSiteMap(
                        response.contentType() != null ? response.contentType() : "text/xml",
                        response.body(),
                        uri.toURL()
                );

                if (sitemap instanceof SiteMapIndex index) {
                    for (AbstractSiteMap child : index.getSitemaps()) {
                        String childUrl = child.getUrl().toString();
                        if (seenSitemaps.add(childUrl)) {
                            queue.add(childUrl);
                        }
                    }
                } else if (sitemap instanceof SiteMap pages) {
                    for (SiteMapURL entry : pages.getSiteMapUrls()) {
                        String normalized = urlNormalizer.normalize(entry.getUrl().toString());
                        if (normalized != null && inScope.test(normalized)) {
                            pageUrls.add(normalized);
                            if (pageUrls.size() >= limit) {
                                break;
                            }
                        }
                    }
                }
            } catch (IllegalArgumentException | IOException | UnknownFormatException ex) {
                log.debug("Skipping sitemap: url={}, error={}", sitemapUrl, ex.getMessage());
            }
        }

        return List.copyOf(pageUrls);
    }
}
