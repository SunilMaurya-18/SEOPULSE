package com.seopulse.website.crawler;

import com.seopulse.website.crawler.CrawledPage.Outcome;
import com.seopulse.website.crawler.HostThrottle.CrawlDeadlineException;
import com.seopulse.website.crawler.net.BlockedAddressException;
import com.seopulse.website.crawler.net.CrawlerHttpClient;
import com.seopulse.website.crawler.net.FetchResponse;
import com.seopulse.website.crawler.robots.RobotsTxtService;
import com.seopulse.website.service.UrlValidator;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRules;
import crawlercommons.robots.SimpleRobotRules.RobotRulesMode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Crawls one website: robots.txt is honoured, sitemaps seed the frontier,
 * redirects are followed hop by hop, and requests to each host are spaced
 * by the robots.txt crawl-delay (clamped) across concurrent workers.
 */
@Component
@Slf4j
public class WebsiteCrawler {

    private static final String HTML_ACCEPT =
            "text/html,application/xhtml+xml;q=0.9,*/*;q=0.1";

    private final CrawlerProperties properties;
    private final UrlValidator urlValidator;
    private final UrlNormalizer urlNormalizer;
    private final CrawlerHttpClient httpClient;
    private final RobotsTxtService robotsTxtService;
    private final SitemapService sitemapService;

    public WebsiteCrawler(
            CrawlerProperties properties,
            UrlValidator urlValidator,
            UrlNormalizer urlNormalizer,
            CrawlerHttpClient httpClient,
            RobotsTxtService robotsTxtService,
            SitemapService sitemapService
    ) {
        this.properties = properties;
        this.urlValidator = urlValidator;
        this.urlNormalizer = urlNormalizer;
        this.httpClient = httpClient;
        this.robotsTxtService = robotsTxtService;
        this.sitemapService = sitemapService;
    }

    /**
     * Crawls a website starting from the supplied URL.
     *
     * @throws IllegalArgumentException if the start URL is invalid or not public
     */
    public CrawlResult crawl(String startUrl) throws InterruptedException {

        URI validatedStartUrl = urlValidator.validate(startUrl);

        String normalizedStartUrl =
                urlNormalizer.normalize(validatedStartUrl.toString());

        if (normalizedStartUrl == null) {
            throw new IllegalArgumentException("Unable to normalize website URL");
        }

        log.info(
                "Starting website crawl: url={}, maxPages={}, maxDepth={}, concurrency={}",
                normalizedStartUrl,
                properties.getMaxPages(),
                properties.getMaxDepth(),
                properties.getConcurrency()
        );

        CrawlResult result = new CrawlSession(normalizedStartUrl, validatedStartUrl.getHost()).run();

        log.info(
                "Website crawl finished: startUrl={}, pages={}, timedOut={}",
                normalizedStartUrl,
                result.pages().size(),
                result.timedOut()
        );

        return result;
    }

    private final class CrawlSession {

        private final String startUrl;
        private final SiteScope scope;
        private final HostThrottle throttle = new HostThrottle();
        private final Map<String, BaseRobotRules> robotsByOrigin = new ConcurrentHashMap<>();
        private final Set<String> visited = ConcurrentHashMap.newKeySet();
        private final Queue<CrawledPage> pages = new ConcurrentLinkedQueue<>();
        private final BlockingQueue<CrawlTarget> frontier = new LinkedBlockingQueue<>();
        private final AtomicInteger pending = new AtomicInteger();
        private final AtomicInteger slots = new AtomicInteger();
        private final AtomicBoolean timedOut = new AtomicBoolean();
        private final long deadlineNanos;
        private final int maxPages;
        private final int maxVisited;

        private volatile String startFinalUrl;

        CrawlSession(String startUrl, String startHost) {
            this.startUrl = startUrl;
            this.startFinalUrl = startUrl;
            this.scope = new SiteScope(startHost);
            this.deadlineNanos = System.nanoTime()
                    + TimeUnit.MINUTES.toNanos(properties.getMaxDurationMinutes());
            this.maxPages = Math.max(1, properties.getMaxPages());
            this.maxVisited = maxPages * 5;
        }

        CrawlResult run() throws InterruptedException {

            visited.add(startUrl);

            // The start URL goes first so that its redirect target joins the site scope
            // and robots.txt is loaded before any page request.
            process(new CrawlTarget(startUrl, 0), true);

            seedFromSitemaps();
            runWorkers();

            return new CrawlResult(List.copyOf(pages), timedOut.get());
        }

        private void seedFromSitemaps() throws InterruptedException {

            if (deadlinePassed() || properties.getMaxDepth() < 1) {
                return;
            }

            try {
                URI start = URI.create(startFinalUrl);

                if (!isAllowedByRobots(start)) {
                    return;
                }

                List<String> urls = sitemapService.discover(
                        RobotsTxtService.origin(start),
                        rules(start).getSitemaps(),
                        scope::contains,
                        maxPages,
                        this::pageGate
                );

                urls.forEach(url -> enqueue(url, 1));

                log.debug("Seeded {} URLs from sitemaps: startUrl={}", urls.size(), startFinalUrl);
            } catch (CrawlDeadlineException ex) {
                timedOut.set(true);
            }
        }

        private void runWorkers() throws InterruptedException {

            int workerCount = Math.max(1, properties.getConcurrency());
            List<Thread> workers = new ArrayList<>(workerCount);

            for (int i = 0; i < workerCount; i++) {
                workers.add(Thread.ofVirtual().name("crawler-", i).start(this::workLoop));
            }

            try {
                for (Thread worker : workers) {
                    worker.join();
                }
            } catch (InterruptedException ex) {
                workers.forEach(Thread::interrupt);
                throw ex;
            }
        }

        private void workLoop() {

            while (!Thread.currentThread().isInterrupted()) {

                if (deadlinePassed()) {
                    if (pending.get() > 0) {
                        timedOut.set(true);
                    }
                    return;
                }

                if (slots.get() >= maxPages) {
                    return;
                }

                CrawlTarget target;

                try {
                    target = frontier.poll(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                    return;
                }

                if (target == null) {
                    if (pending.get() == 0) {
                        return;
                    }
                    continue;
                }

                try {
                    process(target, false);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (RuntimeException ex) {
                    log.warn("Unexpected crawler error: url={}, error={}", target.url(), ex.toString());
                } finally {
                    pending.decrementAndGet();
                }
            }
        }

        private void process(CrawlTarget target, boolean isStart) throws InterruptedException {

            URI uri;

            try {
                uri = urlValidator.validateStructure(target.url());
            } catch (IllegalArgumentException ex) {
                return;
            }

            if (!reserveSlot()) {
                return;
            }

            try {
                if (!isAllowedByRobots(uri)) {
                    record(CrawledPage.withoutContent(
                            target.url(), Outcome.SKIPPED_ROBOTS, 0, null, target.depth(),
                            null, null, "Disallowed by robots.txt"
                    ));
                    return;
                }

                fetchAndRecord(target, uri, isStart);

            } catch (CrawlDeadlineException ex) {
                slots.decrementAndGet();
                timedOut.set(true);
            } catch (BlockedAddressException ex) {
                record(CrawledPage.withoutContent(
                        target.url(), Outcome.FAILED, 0, null, target.depth(),
                        null, null, "Blocked: host resolves to a restricted network address"
                ));
            } catch (IOException ex) {
                record(CrawledPage.withoutContent(
                        target.url(), Outcome.FAILED, 0, null, target.depth(),
                        null, null, failureReason(ex)
                ));
            }
        }

        private void fetchAndRecord(
                CrawlTarget target,
                URI uri,
                boolean isStart
        ) throws IOException, InterruptedException {

            List<String> chain = new ArrayList<>();
            chain.add(target.url());

            URI current = uri;
            FetchResponse response = fetchWithRetry(current);
            int firstStatus = response.status();

            while (response.isRedirect() && response.location() != null) {

                String next;

                try {
                    next = urlNormalizer.normalize(current.resolve(response.location().trim()).toString());
                } catch (IllegalArgumentException ex) {
                    next = null;
                }

                if (next == null) {
                    recordRedirect(target, firstStatus, null, chain, "Invalid redirect location");
                    return;
                }

                if (isStart) {
                    scope.addHost(URI.create(next).getHost());
                }

                if (!scope.contains(next)) {
                    recordRedirect(target, firstStatus, next, append(chain, next), "Redirects to another site");
                    return;
                }

                if (chain.contains(next)) {
                    recordRedirect(target, firstStatus, next, append(chain, next), "Redirect loop");
                    return;
                }

                chain.add(next);

                if (chain.size() - 1 > properties.getMaxRedirects()) {
                    recordRedirect(target, firstStatus, next, chain, "Too many redirects");
                    return;
                }

                URI nextUri;

                try {
                    nextUri = urlValidator.validateStructure(next);
                } catch (IllegalArgumentException ex) {
                    recordRedirect(target, firstStatus, next, chain, "Redirect target not allowed: " + ex.getMessage());
                    return;
                }

                if (!isAllowedByRobots(nextUri)) {
                    recordRedirect(target, firstStatus, next, chain, "Redirect target disallowed by robots.txt");
                    return;
                }

                current = nextUri;
                response = fetchWithRetry(current);
            }

            String finalUrl = chain.getLast();

            if (chain.size() > 1) {

                recordRedirect(target, firstStatus, finalUrl, chain, null);

                if (isStart) {
                    startFinalUrl = finalUrl;
                }

                if (!visited.add(finalUrl) || !reserveSlot()) {
                    return;
                }
            }

            recordFinal(finalUrl, target.depth(), response);
        }

        private void recordFinal(String url, int depth, FetchResponse response) throws IOException {

            String contentType = response.contentType();

            if (response.tooLarge()) {
                record(CrawledPage.withoutContent(
                        url, Outcome.TOO_LARGE, response.status(), contentType, depth,
                        null, null, "Response exceeded " + properties.getMaxBodySizeBytes() + " bytes"
                ));
                return;
            }

            if (!response.isSuccess()) {
                record(CrawledPage.withoutContent(
                        url, Outcome.CRAWLED, response.status(), contentType, depth,
                        null, null, null
                ));
                return;
            }

            if (!isHtml(contentType)) {
                slots.decrementAndGet();
                return;
            }

            Document document = parseHtml(response.body(), contentType, url);

            Set<String> links = new LinkedHashSet<>();
            int internalLinks = 0;
            int externalLinks = 0;

            for (Element anchor : document.select("a[href]")) {

                String normalized = urlNormalizer.normalize(anchor.absUrl("href"));

                if (normalized == null) {
                    continue;
                }

                if (scope.contains(normalized)) {
                    internalLinks++;
                    links.add(normalized);
                } else {
                    externalLinks++;
                }
            }

            record(new CrawledPage(
                    url,
                    Outcome.CRAWLED,
                    response.status(),
                    contentType,
                    extractTitle(document),
                    extractMetaDescription(document),
                    extractCanonical(document),
                    countWords(document),
                    document.select("h1").size(),
                    document.select("img").size(),
                    countImagesWithoutAlt(document),
                    internalLinks,
                    externalLinks,
                    depth,
                    links,
                    null,
                    null,
                    null
            ));

            if (depth < properties.getMaxDepth()) {
                links.forEach(link -> enqueue(link, depth + 1));
            }
        }

        private FetchResponse fetchWithRetry(URI uri) throws IOException, InterruptedException {

            for (int attempt = 0; ; attempt++) {

                pageGate(uri);

                FetchResponse response = httpClient.fetch(
                        uri,
                        HTML_ACCEPT,
                        properties.getMaxBodySizeBytes(),
                        WebsiteCrawler::isHtml
                );

                boolean throttled = response.status() == 429 || response.status() == 503;

                if (!throttled || attempt >= properties.getMaxRetries()) {
                    return response;
                }

                long waitMs = retryAfterMs(response.retryAfter());

                log.debug("Server asked to slow down: url={}, status={}, waitMs={}", uri, response.status(), waitMs);

                throttle.penalize(uri.getHost(), waitMs);
            }
        }

        private void pageGate(URI uri) throws InterruptedException {
            throttle.acquire(uri.getHost(), delayFor(uri), deadlineNanos);
        }

        private void robotsGate(URI uri) throws InterruptedException {
            throttle.acquire(uri.getHost(), properties.getMinDelayMs(), deadlineNanos);
        }

        private boolean isAllowedByRobots(URI uri) {
            return !properties.isRespectRobotsTxt() || rules(uri).isAllowed(uri.toString());
        }

        private BaseRobotRules rules(URI uri) {
            return robotsByOrigin.computeIfAbsent(RobotsTxtService.origin(uri), origin -> {
                try {
                    return robotsTxtService.rulesFor(uri, this::robotsGate);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return new SimpleRobotRules(RobotRulesMode.ALLOW_NONE);
                }
            });
        }

        private long delayFor(URI uri) {

            long crawlDelay = properties.isRespectRobotsTxt()
                    ? rules(uri).getCrawlDelay()
                    : BaseRobotRules.UNSET_CRAWL_DELAY;

            if (crawlDelay == BaseRobotRules.UNSET_CRAWL_DELAY || crawlDelay < 0) {
                return properties.getMinDelayMs();
            }

            return Math.max(
                    properties.getMinDelayMs(),
                    Math.min(crawlDelay, properties.getMaxCrawlDelayMs())
            );
        }

        private void enqueue(String url, int depth) {

            if (depth > properties.getMaxDepth() || visited.size() >= maxVisited) {
                return;
            }

            if (visited.add(url)) {
                pending.incrementAndGet();
                frontier.add(new CrawlTarget(url, depth));
            }
        }

        private boolean reserveSlot() {
            while (true) {
                int current = slots.get();
                if (current >= maxPages) {
                    return false;
                }
                if (slots.compareAndSet(current, current + 1)) {
                    return true;
                }
            }
        }

        private void recordRedirect(
                CrawlTarget target,
                int status,
                String finalUrl,
                List<String> chain,
                String skipReason
        ) {
            record(CrawledPage.withoutContent(
                    target.url(), Outcome.REDIRECT, status, null, target.depth(),
                    finalUrl, List.copyOf(chain), skipReason
            ));
        }

        private void record(CrawledPage page) {

            pages.add(page);

            log.debug(
                    "Page recorded: url={}, outcome={}, status={}, depth={}",
                    page.url(),
                    page.outcome(),
                    page.status(),
                    page.depth()
            );
        }

        private boolean deadlinePassed() {
            return System.nanoTime() - deadlineNanos > 0;
        }
    }

    private long retryAfterMs(String header) {

        long defaultMs = TimeUnit.SECONDS.toMillis(properties.getDefaultRetryAfterSeconds());
        long maxMs = TimeUnit.SECONDS.toMillis(properties.getMaxRetryAfterSeconds());
        long waitMs = defaultMs;

        if (header != null && !header.isBlank()) {
            try {
                waitMs = TimeUnit.SECONDS.toMillis(Long.parseLong(header.trim()));
            } catch (NumberFormatException notSeconds) {
                try {
                    ZonedDateTime retryAt = ZonedDateTime.parse(header.trim(), DateTimeFormatter.RFC_1123_DATE_TIME);
                    waitMs = Duration.between(Instant.now(), retryAt.toInstant()).toMillis();
                } catch (DateTimeParseException notDate) {
                    waitMs = defaultMs;
                }
            }
        }

        return Math.min(maxMs, Math.max(properties.getMinDelayMs(), waitMs));
    }

    private static Document parseHtml(byte[] body, String contentType, String baseUrl) throws IOException {
        return Jsoup.parse(new ByteArrayInputStream(body), charsetFrom(contentType), baseUrl);
    }

    /**
     * Returns the charset declared in the Content-Type header, or null so
     * that jsoup detects it from the BOM or {@code <meta charset>}.
     */
    static String charsetFrom(String contentType) {

        if (contentType == null) {
            return null;
        }

        for (String parameter : contentType.split(";")) {

            String trimmed = parameter.trim();

            if (trimmed.regionMatches(true, 0, "charset=", 0, 8)) {

                String charset = trimmed.substring(8).trim().replace("\"", "").replace("'", "");

                try {
                    return Charset.isSupported(charset) ? charset : null;
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }
        }

        return null;
    }

    static boolean isHtml(String contentType) {

        if (contentType == null) {
            return false;
        }

        String normalized = contentType.toLowerCase().trim();

        return normalized.startsWith("text/html")
                || normalized.startsWith("application/xhtml+xml");
    }

    private static String failureReason(IOException ex) {
        String message = ex.getMessage();
        String reason = ex.getClass().getSimpleName() + (message != null ? ": " + message : "");
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }

    private static List<String> append(List<String> chain, String url) {
        List<String> copy = new ArrayList<>(chain);
        copy.add(url);
        return copy;
    }

    private String extractTitle(Document document) {

        Element title = document.selectFirst("title");

        if (title == null) {
            return null;
        }

        String value = title.text().trim();

        return value.isBlank() ? null : value;
    }

    private String extractMetaDescription(Document document) {

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

    private int countWords(Document document) {

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
    private int countImagesWithoutAlt(Document document) {

        int count = 0;

        for (Element image : document.select("img")) {
            if (image.attr("alt").isBlank()) {
                count++;
            }
        }

        return count;
    }

    private record CrawlTarget(String url, int depth) {
    }
}
