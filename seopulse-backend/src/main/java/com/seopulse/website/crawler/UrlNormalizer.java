package com.seopulse.website.crawler;

import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class UrlNormalizer {

    /**
     * Lowercases scheme and host, drops default ports, user info and the
     * fragment, and keeps the path and query exactly as encoded.
     */
    public String normalize(String rawUrl) {

        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        try {

            URI uri = URI.create(rawUrl.trim());

            String scheme = uri.getScheme();

            if (scheme == null) {
                return null;
            }

            String normalizedScheme =
                    scheme.toLowerCase();

            if (!normalizedScheme.equals("http")
                    && !normalizedScheme.equals("https")) {
                return null;
            }

            String host = uri.getHost();

            if (host == null || host.isBlank()) {
                return null;
            }

            String normalizedHost =
                    host.toLowerCase();

            int port = uri.getPort();

            if ((port == 80 && normalizedScheme.equals("http"))
                    || (port == 443 && normalizedScheme.equals("https"))) {
                port = -1;
            }

            String path = uri.getRawPath();

            if (path == null || path.isBlank()) {
                path = "/";
            }

            StringBuilder normalized = new StringBuilder()
                    .append(normalizedScheme)
                    .append("://")
                    .append(normalizedHost);

            if (port != -1) {
                normalized.append(':').append(port);
            }

            normalized.append(path);

            if (uri.getRawQuery() != null) {
                normalized.append('?').append(uri.getRawQuery());
            }

            return normalized.toString();

        } catch (Exception ex) {
            return null;
        }
    }
}
