package com.seopulse.website.seo.analyzer.site;

import com.seopulse.website.crawler.net.BlockedAddressException;
import com.seopulse.website.crawler.net.CrawlerHttpClient;
import com.seopulse.website.entity.AuditPage;
import com.seopulse.website.service.UrlValidator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Checks a bounded sample of outbound links with HEAD requests. Requests
 * go through the crawler's SSRF-safe client, at most a few per host, and
 * within an overall time budget so a slow third party cannot stall the audit.
 */
@Component
@Slf4j
public class ExternalLinkChecker {

    /** Status recorded when the link's host no longer resolves. */
    public static final int UNRESOLVABLE = -1;

    private final CrawlerHttpClient httpClient;
    private final UrlValidator urlValidator;
    private final Settings settings;

    public ExternalLinkChecker(
            CrawlerHttpClient httpClient,
            UrlValidator urlValidator,
            Settings settings
    ) {
        this.httpClient = httpClient;
        this.urlValidator = urlValidator;
        this.settings = settings;
    }

    @Component
    @ConfigurationProperties(prefix = "seopulse.analysis.external-links")
    @Getter
    @Setter
    public static class Settings {
        /** Maximum external URLs checked per audit; 0 disables the check. */
        private int maxChecks = 50;
        private int maxPerHost = 3;
        private int concurrency = 4;
        private long budgetSeconds = 60;
    }

    public static boolean isBroken(Integer status) {
        if (status == null) {
            return false;
        }
        return status == UNRESOLVABLE || status == 404 || status == 410 || status >= 500;
    }

    public Map<String, Integer> check(List<AuditPage> pages) {

        if (settings.getMaxChecks() <= 0) {
            return Map.of();
        }

        List<URI> targets = selectTargets(pages);

        if (targets.isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> results = new ConcurrentHashMap<>();
        Semaphore permits = new Semaphore(Math.max(1, settings.getConcurrency()));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(settings.getBudgetSeconds());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (URI target : targets) {
                futures.add(executor.submit(() -> {
                    if (System.nanoTime() > deadline || !permits.tryAcquire(remainingMs(deadline), TimeUnit.MILLISECONDS)) {
                        return null;
                    }
                    try {
                        Integer status = statusOf(target);
                        if (status != null) {
                            results.put(target.toString(), status);
                        }
                    } finally {
                        permits.release();
                    }
                    return null;
                }));
            }

            for (Future<?> future : futures) {
                long remaining = remainingMs(deadline);
                try {
                    future.get(Math.max(1, remaining), TimeUnit.MILLISECONDS);
                } catch (java.util.concurrent.TimeoutException ex) {
                    futures.forEach(pending -> pending.cancel(true));
                    break;
                } catch (java.util.concurrent.ExecutionException ex) {
                    log.debug("External link check failed", ex.getCause());
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        log.info("External links checked: requested={}, completed={}", targets.size(), results.size());
        return new HashMap<>(results);
    }

    private List<URI> selectTargets(List<AuditPage> pages) {
        Set<String> seen = new LinkedHashSet<>();
        Map<String, Integer> perHost = new HashMap<>();
        List<URI> targets = new ArrayList<>();

        for (AuditPage page : pages) {
            if (page.getSignals() == null) {
                continue;
            }
            for (String link : page.getSignals().externalLinksOrEmpty()) {
                if (targets.size() >= settings.getMaxChecks()) {
                    return targets;
                }
                if (!seen.add(link)) {
                    continue;
                }
                try {
                    URI uri = urlValidator.validateStructure(link);
                    String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
                    if (perHost.merge(host, 1, Integer::sum) <= settings.getMaxPerHost()) {
                        targets.add(uri);
                    }
                } catch (IllegalArgumentException ignored) {
                    // mailto:, non-standard ports and other links we never fetch
                }
            }
        }
        return targets;
    }

    private Integer statusOf(URI uri) throws InterruptedException {
        try {
            return httpClient.status(uri);
        } catch (BlockedAddressException ex) {
            return null;
        } catch (UnknownHostException ex) {
            return UNRESOLVABLE;
        } catch (IOException ex) {
            // Timeouts and resets are often transient; do not report them.
            return null;
        }
    }

    private static long remainingMs(long deadlineNanos) {
        return Math.max(0, TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime()));
    }
}
