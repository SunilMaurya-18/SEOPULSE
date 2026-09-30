package com.seopulse.website.service;

import com.seopulse.website.crawler.CrawlerProperties;
import com.seopulse.website.crawler.net.NetworkPolicy;
import com.seopulse.website.crawler.net.SafeSocketAddressResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlValidatorTest {

    private static final Map<String, String> DNS = Map.of(
            "public.test", "93.184.216.34",
            "internal.test", "10.0.0.5",
            "metadata.test", "169.254.169.254",
            "mapped.test", "::ffff:127.0.0.1"
    );

    private final CrawlerProperties properties = new CrawlerProperties();

    private final UrlValidator validator = new UrlValidator(
            new SafeSocketAddressResolver(
                    host -> List.of(InetAddress.getByName(DNS.getOrDefault(host, host))),
                    new NetworkPolicy(properties)
            ),
            properties
    );

    @ParameterizedTest
    @ValueSource(strings = {
            "http://public.test/",
            "https://public.test/path?q=1",
            "https://public.test:443/",
            "http://public.test:80/"
    })
    void acceptsPublicHttpUrls(String url) {
        assertThat(validator.validate(url).getHost()).isEqualTo("public.test");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://public.test/",
            "file:///etc/passwd",
            "javascript:alert(1)",
            "http://user:pass@public.test/",
            "http://public.test:8080/",
            "https://public.test:22/",
            "http:///nohost",
            "not a url"
    })
    void rejectsInvalidStructure(String url) {
        assertThatThrownBy(() -> validator.validateStructure(url))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://internal.test/",
            "http://metadata.test/latest/meta-data/",
            "http://mapped.test/",
            "http://127.0.0.1/",
            "http://[::1]/",
            "http://0.0.0.0/"
    })
    void rejectsRestrictedAddresses(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("restricted");
    }

    @Test
    void rejectsUnresolvableHost() {

        UrlValidator failing = new UrlValidator(
                new SafeSocketAddressResolver(
                        host -> {
                            throw new java.net.UnknownHostException(host);
                        },
                        new NetworkPolicy(properties)
                ),
                properties
        );

        assertThatThrownBy(() -> failing.validate("http://nowhere.test/"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("could not be resolved");
    }
}
