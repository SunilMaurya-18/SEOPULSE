package com.seopulse.website.crawler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Per-host politeness: requests to the same host are spaced at least
 * {@code delayMs} apart, even across concurrent workers.
 */
final class HostThrottle {

    private final Map<String, Long> nextAllowedNanos = new HashMap<>();

    /**
     * Reserves the next request slot for {@code host} and sleeps until it.
     *
     * @throws CrawlDeadlineException if the slot is after the deadline
     */
    void acquire(String host, long delayMs, long deadlineNanos) throws InterruptedException {

        long waitNanos;

        synchronized (this) {
            long now = System.nanoTime();
            long slot = Math.max(now, nextAllowedNanos.getOrDefault(host, now));

            if (slot - deadlineNanos > 0) {
                throw new CrawlDeadlineException();
            }

            nextAllowedNanos.put(host, slot + TimeUnit.MILLISECONDS.toNanos(delayMs));
            waitNanos = slot - now;
        }

        if (waitNanos > 0) {
            TimeUnit.NANOSECONDS.sleep(waitNanos);
        }
    }

    /**
     * Pushes the next slot for {@code host} at least {@code delayMs} into the future.
     */
    synchronized void penalize(String host, long delayMs) {
        long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayMs);
        nextAllowedNanos.merge(host, until, (current, candidate) -> candidate - current > 0 ? candidate : current);
    }

    static final class CrawlDeadlineException extends RuntimeException {
        CrawlDeadlineException() {
            super("Crawl time budget exhausted", null, false, false);
        }
    }
}
