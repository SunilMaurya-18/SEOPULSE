package com.seopulse.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * A local web server hosting small test websites. Each site lives under
 * its own path prefix so tests do not interfere with each other.
 */
public final class TestSite {

    private final HttpServer server;
    private final Map<String, String> pages = new ConcurrentHashMap<>();
    private final Map<String, Duration> delays = new ConcurrentHashMap<>();

    private TestSite(HttpServer server) {
        this.server = server;
    }

    public static TestSite start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            TestSite site = new TestSite(server);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.createContext("/", site::handle);
            server.start();
            return site;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public int port() {
        return server.getAddress().getPort();
    }

    /**
     * Registers pages (path relative to the site root, e.g. "" or "about")
     * and returns the site's root URL.
     */
    public String createSite(Map<String, String> sitePages) {
        return createSite(sitePages, Duration.ZERO);
    }

    public String createSite(Map<String, String> sitePages, Duration delayPerPage) {
        String prefix = "/site-" + UUID.randomUUID() + "/";
        sitePages.forEach((path, html) -> pages.put(prefix + path, html));
        if (!delayPerPage.isZero()) {
            delays.put(prefix, delayPerPage);
        }
        return "http://localhost:" + port() + prefix;
    }

    public static String page(String title, String description, String body) {
        return """
                <!doctype html>
                <html><head>
                <title>%s</title>
                %s
                </head><body>%s</body></html>
                """.formatted(
                title,
                description == null ? "" : "<meta name=\"description\" content=\"" + description + "\">",
                body
        );
    }

    private void handle(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();

        delays.forEach((prefix, delay) -> {
            if (path.startsWith(prefix)) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        String html = pages.get(path);

        byte[] body = (html != null ? html : "Not found").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", html != null ? "text/html; charset=utf-8" : "text/plain");
        exchange.sendResponseHeaders(html != null ? 200 : 404, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
