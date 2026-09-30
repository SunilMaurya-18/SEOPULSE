package com.seopulse.website.crawler;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The set of hosts that belong to the audited site. {@code example.com}
 * and {@code www.example.com} are treated as the same site.
 */
public final class SiteScope {

    private final Set<String> siteKeys = ConcurrentHashMap.newKeySet();

    public SiteScope(String host) {
        addHost(host);
    }

    public void addHost(String host) {
        String key = siteKey(host);
        if (key != null) {
            siteKeys.add(key);
        }
    }

    public boolean containsHost(String host) {
        String key = siteKey(host);
        return key != null && siteKeys.contains(key);
    }

    public boolean contains(String url) {
        try {
            return containsHost(URI.create(url).getHost());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    static String siteKey(String host) {

        if (host == null || host.isBlank()) {
            return null;
        }

        String key = host.trim().toLowerCase();

        if (key.endsWith(".")) {
            key = key.substring(0, key.length() - 1);
        }

        if (key.startsWith("www.")) {
            key = key.substring(4);
        }

        return key.isEmpty() ? null : key;
    }
}
