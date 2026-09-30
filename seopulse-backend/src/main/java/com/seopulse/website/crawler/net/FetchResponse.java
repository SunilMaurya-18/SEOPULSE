package com.seopulse.website.crawler.net;

/**
 * Result of a single HTTP exchange. {@code body} is empty when the body
 * was not requested or exceeded the size limit ({@code tooLarge}).
 */
public record FetchResponse(
        int status,
        String contentType,
        String location,
        String retryAfter,
        byte[] body,
        boolean tooLarge
) {

    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }

    public boolean isRedirect() {
        return status == 301
                || status == 302
                || status == 303
                || status == 307
                || status == 308;
    }
}
