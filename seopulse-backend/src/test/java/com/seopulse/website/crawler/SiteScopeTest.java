package com.seopulse.website.crawler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SiteScopeTest {

    @Test
    void treatsWwwAndApexAsSameSite() {

        SiteScope scope = new SiteScope("example.com");

        assertThat(scope.contains("https://www.example.com/page")).isTrue();
        assertThat(scope.contains("http://EXAMPLE.com./")).isTrue();
        assertThat(scope.contains("https://blog.example.com/")).isFalse();
        assertThat(scope.contains("https://example.org/")).isFalse();
        assertThat(scope.contains("https://notexample.com/")).isFalse();
    }

    @Test
    void addedHostsJoinTheScope() {

        SiteScope scope = new SiteScope("www.example.com");
        scope.addHost("shop.example.com");

        assertThat(scope.contains("https://example.com/")).isTrue();
        assertThat(scope.contains("https://shop.example.com/cart")).isTrue();
    }

    @Test
    void normalizerDropsDefaultPortsAndFragments() {

        UrlNormalizer normalizer = new UrlNormalizer();

        assertThat(normalizer.normalize("HTTPS://Example.com:443/a%20b?x=1#top"))
                .isEqualTo("https://example.com/a%20b?x=1");
        assertThat(normalizer.normalize("http://example.com:80"))
                .isEqualTo("http://example.com/");
        assertThat(normalizer.normalize("http://example.com:8080/"))
                .isEqualTo("http://example.com:8080/");
        assertThat(normalizer.normalize("mailto:someone@example.com")).isNull();
    }

    @Test
    void charsetIsTakenFromContentTypeWhenSupported() {

        assertThat(WebsiteCrawler.charsetFrom("text/html; charset=ISO-8859-1")).isEqualTo("ISO-8859-1");
        assertThat(WebsiteCrawler.charsetFrom("text/html; charset=\"utf-8\"")).isEqualTo("utf-8");
        assertThat(WebsiteCrawler.charsetFrom("text/html; charset=bogus-charset")).isNull();
        assertThat(WebsiteCrawler.charsetFrom("text/html")).isNull();
    }
}
