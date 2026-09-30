package com.seopulse.website.service;

import com.seopulse.website.crawler.CrawlerProperties;
import com.seopulse.website.crawler.net.BlockedAddressException;
import com.seopulse.website.crawler.net.SafeSocketAddressResolver;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.UnknownHostException;

@Component
public class UrlValidator {

    private final SafeSocketAddressResolver addressResolver;
    private final CrawlerProperties properties;

    public UrlValidator(
            SafeSocketAddressResolver addressResolver,
            CrawlerProperties properties
    ) {
        this.addressResolver = addressResolver;
        this.properties = properties;
    }

    /**
     * Validates the URL structure and checks that its host resolves only
     * to public addresses. Use this for user input; the crawler re-checks
     * addresses on every connection.
     */
    public URI validate(String rawUrl) {

        URI uri = validateStructure(rawUrl);

        try {
            addressResolver.resolveAllowed(uri.getHost());
        } catch (BlockedAddressException ex) {
            throw new IllegalArgumentException(
                    "Website URL resolves to a restricted network address"
            );
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException(
                    "Website hostname could not be resolved"
            );
        }

        return uri;
    }

    /**
     * Validates scheme, host, port and user info without DNS resolution.
     */
    public URI validateStructure(String rawUrl) {

        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Website URL is required"
            );
        }

        URI uri;

        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid website URL"
            );
        }

        validateScheme(uri);
        validateHost(uri);
        validateUserInfo(uri);
        validatePort(uri);

        return uri;
    }

    private void validateScheme(URI uri) {

        String scheme = uri.getScheme();

        if (scheme == null ||
                (!scheme.equalsIgnoreCase("http")
                        && !scheme.equalsIgnoreCase("https"))) {

            throw new IllegalArgumentException(
                    "Only HTTP and HTTPS URLs are allowed"
            );
        }
    }

    private void validateHost(URI uri) {

        String host = uri.getHost();

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException(
                    "Website URL must contain a valid host"
            );
        }

        if (host.length() > 253) {
            throw new IllegalArgumentException(
                    "Website hostname is too long"
            );
        }
    }

    private void validateUserInfo(URI uri) {

        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "URLs containing user information are not allowed"
            );
        }
    }

    private void validatePort(URI uri) {

        int port = uri.getPort() != -1
                ? uri.getPort()
                : uri.getScheme().equalsIgnoreCase("https") ? 443 : 80;

        if (!properties.getAllowedPorts().contains(port)) {
            throw new IllegalArgumentException(
                    "Only standard HTTP and HTTPS ports are allowed"
            );
        }
    }
}
