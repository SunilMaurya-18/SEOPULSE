package com.seopulse.website.crawler.net;

import com.seopulse.website.crawler.CrawlerProperties;
import com.seopulse.website.service.UrlValidator;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.InputStreamResponseListener;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.http.HttpCookieStore;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * HTTP client for all crawler traffic. Redirects are never followed
 * automatically so that every hop can be validated, and every connection
 * goes through {@link SafeSocketAddressResolver}.
 */
@Component
public class CrawlerHttpClient implements DisposableBean {

    private static final int DRAIN_LIMIT_BYTES = 64 * 1024;

    private final HttpClient httpClient;
    private final UrlValidator urlValidator;
    private final long requestTimeoutMs;

    public CrawlerHttpClient(
            CrawlerProperties properties,
            SafeSocketAddressResolver addressResolver,
            UrlValidator urlValidator
    ) throws Exception {

        this.urlValidator = urlValidator;
        this.requestTimeoutMs = properties.getRequestTimeoutMs();

        HttpClient client = new HttpClient();
        client.setFollowRedirects(false);
        client.setSocketAddressResolver(addressResolver);
        client.setUserAgentField(new HttpField(HttpHeader.USER_AGENT, properties.getUserAgent()));
        client.setConnectTimeout(properties.getConnectTimeoutMs());
        client.setIdleTimeout(properties.getRequestTimeoutMs());
        client.setMaxConnectionsPerDestination(Math.max(1, properties.getConcurrency()));
        client.setHttpCookieStore(new HttpCookieStore.Empty());
        client.start();

        this.httpClient = client;
    }

    /**
     * Before-request hook, used for per-host politeness delays.
     */
    @FunctionalInterface
    public interface FetchGate {
        void await(URI uri) throws IOException, InterruptedException;
    }

    /**
     * Fetches {@code uri}, following up to {@code maxHops} redirects.
     * Every hop is re-validated and resolved through the safe resolver.
     * Returns the last response (possibly a redirect if hops ran out).
     */
    public FetchResponse fetchFollowingRedirects(
            URI uri,
            String accept,
            long maxBytes,
            int maxHops,
            FetchGate gate
    ) throws IOException, InterruptedException {

        URI current = uri;

        for (int hop = 0; ; hop++) {

            gate.await(current);

            FetchResponse response = fetch(current, accept, maxBytes, contentType -> true);

            if (!response.isRedirect() || response.location() == null || hop >= maxHops) {
                return response;
            }

            try {
                current = urlValidator.validateStructure(
                        current.resolve(response.location().trim()).toString()
                );
            } catch (IllegalArgumentException ex) {
                return response;
            }
        }
    }

    /**
     * Performs a GET request. The body is read only for 2xx responses whose
     * content type satisfies {@code readBodyFor}; otherwise it is discarded.
     *
     * @throws IOException on network failure, timeout or blocked address
     */
    public FetchResponse fetch(
            URI uri,
            String accept,
            long maxBytes,
            Predicate<String> readBodyFor
    ) throws IOException, InterruptedException {

        InputStreamResponseListener listener = new InputStreamResponseListener();

        httpClient.newRequest(uri)
                .method(HttpMethod.GET)
                .timeout(requestTimeoutMs, TimeUnit.MILLISECONDS)
                .headers(headers -> headers.put(HttpHeader.ACCEPT, accept))
                .send(listener);

        Response response;

        try {
            response = listener.get(requestTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            listener.close();
            throw new SocketTimeoutException("Request timed out: " + uri);
        } catch (ExecutionException ex) {
            throw unwrap(ex, uri);
        }

        HttpFields headers = response.getHeaders();
        String contentType = headers.get(HttpHeader.CONTENT_TYPE);
        boolean readBody = response.getStatus() >= 200
                && response.getStatus() < 300
                && readBodyFor.test(contentType);
        long limit = readBody ? maxBytes : DRAIN_LIMIT_BYTES;

        byte[] body;
        boolean tooLarge = false;

        try (InputStream input = listener.getInputStream()) {
            body = readLimited(input, limit);

            if (body == null) {
                response.abort(new IOException("Response body exceeded " + limit + " bytes"));
                tooLarge = readBody;
                body = new byte[0];
            } else if (!readBody) {
                body = new byte[0];
            }
        } catch (InterruptedIOException ex) {
            throw new SocketTimeoutException("Response body timed out: " + uri);
        }

        return new FetchResponse(
                response.getStatus(),
                contentType,
                headers.get(HttpHeader.LOCATION),
                headers.get(HttpHeader.RETRY_AFTER),
                body,
                tooLarge
        );
    }

    /**
     * Returns null when the stream is longer than {@code maxBytes}.
     */
    private static byte[] readLimited(InputStream input, long maxBytes) throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;

        while ((read = input.read(buffer)) != -1) {
            total += read;

            if (total > maxBytes) {
                return null;
            }

            output.write(buffer, 0, read);
        }

        return output.toByteArray();
    }

    private static IOException unwrap(ExecutionException ex, URI uri) {

        Throwable cause = ex.getCause();

        while (cause != null) {
            if (cause instanceof BlockedAddressException blocked) {
                return blocked;
            }
            if (cause instanceof TimeoutException) {
                return new SocketTimeoutException("Request timed out: " + uri);
            }
            if (cause.getCause() == null || cause.getCause() == cause) {
                break;
            }
            cause = cause.getCause();
        }

        if (ex.getCause() instanceof IOException io) {
            return io;
        }

        return new IOException(
                ex.getCause() != null ? ex.getCause().getMessage() : "Request failed",
                ex.getCause()
        );
    }

    @Override
    public void destroy() throws Exception {
        httpClient.stop();
    }
}
