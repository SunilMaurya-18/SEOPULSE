package com.seopulse.website.crawler;

import com.seopulse.website.crawler.CrawledPage.Outcome;
import com.seopulse.website.crawler.net.CrawlerHttpClient;
import com.seopulse.website.crawler.net.HostResolver;
import com.seopulse.website.crawler.net.NetworkPolicy;
import com.seopulse.website.crawler.net.SafeSocketAddressResolver;
import com.seopulse.website.crawler.robots.RobotsTxtCache;
import com.seopulse.website.crawler.robots.RobotsTxtService;
import com.seopulse.website.service.UrlValidator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end crawler tests against a local HTTP server. Hostnames such as
 * {@code example.test} are mapped to 127.0.0.1 by a fake resolver, and
 * private networks are allowed except in the DNS rebinding test.
 * HTTPS is not exercised here (it would need a trusted test certificate).
 */
class WebsiteCrawlerTest {

    private static final byte[] LOCALHOST = {127, 0, 0, 1};

    private HttpServer server;
    private int port;
    private final Map<String, Function<HttpExchange, Response>> routes = new ConcurrentHashMap<>();
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();
    private final List<CrawlerHttpClient> clients = new ArrayList<>();

    private CrawlerProperties properties;

    @BeforeEach
    void startServer() throws IOException {

        server = HttpServer.create(new InetSocketAddress(InetAddress.getByAddress(LOCALHOST), 0), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/", this::handle);
        server.start();
        port = server.getAddress().getPort();

        properties = new CrawlerProperties();
        properties.setAllowPrivateNetworks(true);
        properties.setAllowedPorts(new ArrayList<>(List.of(80, 443, port)));
        properties.setMinDelayMs(0);
        properties.setConcurrency(2);
        properties.setMaxDurationMinutes(1);
        properties.setRequestTimeoutMs(5000);
        properties.setConnectTimeoutMs(2000);
    }

    @AfterEach
    void stopServer() throws Exception {
        for (CrawlerHttpClient client : clients) {
            client.destroy();
        }
        server.stop(0);
    }

    @Test
    void followsApexToWwwRedirectAndCrawlsTheSite() throws Exception {

        routes.put("/", exchange -> isApex(exchange)
                ? Response.redirect(301, "http://www.example.test:" + port + "/")
                : Response.html("""
                        <html><head><title>Home</title></head><body>
                        <a href="/a">A</a> <a href="/b">B</a>
                        <a href="https://other.test/x">External</a>
                        </body></html>
                        """));
        routes.put("/a", exchange -> Response.html("<a href='/b'>B</a><a href='/c'>C</a>"));
        routes.put("/b", exchange -> Response.html("<h1>B</h1>"));
        routes.put("/c", exchange -> Response.status(404));

        CrawlResult result = crawler(host -> localhost(host)).crawl(url("example.test", "/"));

        Map<String, CrawledPage> pages = byUrl(result);

        CrawledPage start = pages.get(url("example.test", "/"));
        assertThat(start.outcome()).isEqualTo(Outcome.REDIRECT);
        assertThat(start.status()).isEqualTo(301);
        assertThat(start.finalUrl()).isEqualTo(url("www.example.test", "/"));
        assertThat(start.redirectChain()).containsExactly(url("example.test", "/"), url("www.example.test", "/"));

        CrawledPage home = pages.get(url("www.example.test", "/"));
        assertThat(home.outcome()).isEqualTo(Outcome.CRAWLED);
        assertThat(home.title()).isEqualTo("Home");
        assertThat(home.internalLinkCount()).isEqualTo(2);
        assertThat(home.externalLinkCount()).isEqualTo(1);

        assertThat(pages.get(url("www.example.test", "/a")).outcome()).isEqualTo(Outcome.CRAWLED);
        assertThat(pages.get(url("www.example.test", "/b")).outcome()).isEqualTo(Outcome.CRAWLED);
        assertThat(pages.get(url("www.example.test", "/c")).status()).isEqualTo(404);
        assertThat(result.timedOut()).isFalse();
    }

    @Test
    void robotsDisallowedPagesAreNeverRequested() throws Exception {

        routes.put("/robots.txt", exchange -> Response.text("User-agent: *\nDisallow: /private\n"));
        routes.put("/", exchange -> Response.html("<a href='/private/secret'>x</a><a href='/public'>y</a>"));
        routes.put("/public", exchange -> Response.html("<p>ok</p>"));
        routes.put("/private/secret", exchange -> Response.html("<p>secret</p>"));

        CrawlResult result = crawler(host -> localhost(host)).crawl(url("example.test", "/"));

        assertThat(requestedPaths()).doesNotContain("/private/secret");
        assertThat(byUrl(result).get(url("example.test", "/private/secret")).outcome())
                .isEqualTo(Outcome.SKIPPED_ROBOTS);
        assertThat(byUrl(result).get(url("example.test", "/public")).outcome())
                .isEqualTo(Outcome.CRAWLED);
    }

    @Test
    void sitemapUrlsSeedTheFrontier() throws Exception {

        routes.put("/robots.txt", exchange -> Response.text(
                "User-agent: *\nAllow: /\nSitemap: http://example.test:" + port + "/sitemap-index.xml\n"));
        routes.put("/sitemap-index.xml", exchange -> Response.xml("""
                <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                  <sitemap><loc>http://example.test:%d/pages.xml</loc></sitemap>
                </sitemapindex>
                """.formatted(port)));
        routes.put("/pages.xml", exchange -> Response.xml("""
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                  <url><loc>http://example.test:%d/orphan</loc></url>
                  <url><loc>https://elsewhere.test/ignored</loc></url>
                </urlset>
                """.formatted(port)));
        routes.put("/", exchange -> Response.html("<p>no links</p>"));
        routes.put("/orphan", exchange -> Response.html("<title>Orphan</title>"));

        CrawlResult result = crawler(host -> localhost(host)).crawl(url("example.test", "/"));

        CrawledPage orphan = byUrl(result).get(url("example.test", "/orphan"));
        assertThat(orphan.outcome()).isEqualTo(Outcome.CRAWLED);
        assertThat(orphan.depth()).isEqualTo(1);
        assertThat(byUrl(result)).doesNotContainKey("https://elsewhere.test/ignored");
    }

    @Test
    void concurrentWorkersStillSpaceRequestsPerHost() throws Exception {

        properties.setConcurrency(4);
        properties.setMinDelayMs(300);

        StringBuilder links = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int page = i;
            links.append("<a href='/p").append(i).append("'>p</a>");
            routes.put("/p" + i, exchange -> Response.html("<p>page " + page + "</p>"));
        }
        routes.put("/", exchange -> Response.html(links.toString()));

        crawler(host -> localhost(host)).crawl(url("example.test", "/"));

        List<Long> arrivals = requests.stream().map(RecordedRequest::nanos).sorted().toList();
        assertThat(arrivals).hasSizeGreaterThanOrEqualTo(8);

        for (int i = 1; i < arrivals.size(); i++) {
            assertThat(Duration.ofNanos(arrivals.get(i) - arrivals.get(i - 1)).toMillis())
                    .as("gap before request %d", i)
                    .isGreaterThanOrEqualTo(250);
        }
    }

    @Test
    void honoursRetryAfterOnTooManyRequests() throws Exception {

        AtomicInteger attempts = new AtomicInteger();

        routes.put("/", exchange -> attempts.incrementAndGet() == 1
                ? Response.status(429).header("Retry-After", "1")
                : Response.html("<title>Finally</title>"));

        CrawlResult result = crawler(host -> localhost(host)).crawl(url("example.test", "/"));

        CrawledPage home = byUrl(result).get(url("example.test", "/"));
        assertThat(home.status()).isEqualTo(200);
        assertThat(home.title()).isEqualTo("Finally");

        List<Long> homeRequests = requests.stream()
                .filter(request -> request.path().equals("/"))
                .map(RecordedRequest::nanos)
                .toList();
        assertThat(homeRequests).hasSize(2);
        assertThat(Duration.ofNanos(homeRequests.get(1) - homeRequests.get(0)).toMillis())
                .isGreaterThanOrEqualTo(900);
    }

    @Test
    void oversizedPagesAreRecordedAsTooLarge() throws Exception {

        properties.setMaxBodySizeBytes(1_000);
        routes.put("/", exchange -> Response.html("<p>" + "x".repeat(5_000) + "</p>"));

        CrawlResult result = crawler(host -> localhost(host)).crawl(url("example.test", "/"));

        assertThat(byUrl(result).get(url("example.test", "/")).outcome()).isEqualTo(Outcome.TOO_LARGE);
    }

    @Test
    void decodesPagesUsingTheDeclaredCharset() throws Exception {

        routes.put("/", exchange -> new Response(
                200,
                Map.of("Content-Type", "text/html; charset=ISO-8859-1"),
                "<title>Caf\u00e9</title>".getBytes(Charset.forName("ISO-8859-1"))
        ));

        CrawlResult result = crawler(host -> localhost(host)).crawl(url("example.test", "/"));

        assertThat(byUrl(result).get(url("example.test", "/")).title()).isEqualTo("Caf\u00e9");
    }

    @Test
    void dnsRebindingToLoopbackIsBlockedAtConnectTime() throws Exception {

        properties.setAllowPrivateNetworks(false);
        properties.setRespectRobotsTxt(false);
        routes.put("/", exchange -> Response.html("<p>internal</p>"));

        AtomicInteger lookups = new AtomicInteger();
        HostResolver rebinding = host -> lookups.getAndIncrement() == 0
                ? List.of(InetAddress.getByAddress(host, new byte[]{93, (byte) 184, (byte) 216, 34}))
                : localhost(host);

        CrawlResult result = crawler(rebinding).crawl(url("rebind.test", "/"));

        assertThat(requests).isEmpty();

        CrawledPage page = byUrl(result).get(url("rebind.test", "/"));
        assertThat(page.outcome()).isEqualTo(Outcome.FAILED);
        assertThat(page.skipReason()).contains("restricted network address");
    }

    private WebsiteCrawler crawler(HostResolver resolver) throws Exception {

        NetworkPolicy policy = new NetworkPolicy(properties);
        SafeSocketAddressResolver addressResolver = new SafeSocketAddressResolver(resolver, policy);
        UrlValidator urlValidator = new UrlValidator(addressResolver, properties);
        UrlNormalizer normalizer = new UrlNormalizer();
        CrawlerHttpClient httpClient = new CrawlerHttpClient(properties, addressResolver, urlValidator);
        clients.add(httpClient);

        return new WebsiteCrawler(
                properties,
                urlValidator,
                normalizer,
                httpClient,
                new RobotsTxtService(properties, httpClient, new InMemoryRobotsTxtCache()),
                new SitemapService(properties, httpClient, urlValidator, normalizer)
        );
    }

    private static List<InetAddress> localhost(String host) throws UnknownHostException {
        return List.of(InetAddress.getByAddress(host, LOCALHOST));
    }

    private String url(String host, String path) {
        return "http://" + host + ":" + port + path;
    }

    private static boolean isApex(HttpExchange exchange) {
        return exchange.getRequestHeaders().getFirst("Host").startsWith("example.test");
    }

    private List<String> requestedPaths() {
        return requests.stream().map(RecordedRequest::path).toList();
    }

    private static Map<String, CrawledPage> byUrl(CrawlResult result) {
        return result.pages().stream()
                .collect(Collectors.toMap(CrawledPage::url, page -> page, (first, second) -> first));
    }

    private void handle(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();
        requests.add(new RecordedRequest(path, System.nanoTime()));

        Response response = Optional.ofNullable(routes.get(path))
                .map(route -> route.apply(exchange))
                .orElse(Response.status(404));

        response.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
        exchange.sendResponseHeaders(response.status(), response.body().length == 0 ? -1 : response.body().length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response.body());
        } catch (IOException ignored) {
            // The crawler may abort oversized responses mid-body.
        }
    }

    private record RecordedRequest(String path, long nanos) {
    }

    private record Response(int status, Map<String, String> headers, byte[] body) {

        static Response html(String html) {
            return new Response(200, Map.of("Content-Type", "text/html; charset=UTF-8"),
                    html.getBytes(StandardCharsets.UTF_8));
        }

        static Response text(String text) {
            return new Response(200, Map.of("Content-Type", "text/plain"), text.getBytes(StandardCharsets.UTF_8));
        }

        static Response xml(String xml) {
            return new Response(200, Map.of("Content-Type", "application/xml"), xml.getBytes(StandardCharsets.UTF_8));
        }

        static Response status(int status) {
            return new Response(status, Map.of(), new byte[0]);
        }

        static Response redirect(int status, String location) {
            return new Response(status, Map.of("Location", location), new byte[0]);
        }

        Response header(String name, String value) {
            Map<String, String> merged = new ConcurrentHashMap<>(headers);
            merged.put(name, value);
            return new Response(status, merged, body);
        }
    }

    private static final class InMemoryRobotsTxtCache implements RobotsTxtCache {

        private final Map<String, CachedRobotsTxt> entries = new ConcurrentHashMap<>();

        @Override
        public Optional<CachedRobotsTxt> get(String origin) {
            return Optional.ofNullable(entries.get(origin));
        }

        @Override
        public void put(String origin, CachedRobotsTxt robotsTxt, Duration ttl) {
            entries.put(origin, robotsTxt);
        }
    }
}
