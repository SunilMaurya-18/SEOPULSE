package com.seopulse.alert;

import com.seopulse.website.crawler.net.SafeSocketAddressResolver;
import com.seopulse.website.service.UrlValidator;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.InputStreamResponseListener;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.client.StringRequestContent;
import org.eclipse.jetty.http.HttpCookieStore;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Posts alert webhooks. Connections go through {@link SafeSocketAddressResolver}
 * so a webhook URL cannot reach private networks (re-checked on every request,
 * which also defeats DNS rebinding), and redirects are never followed.
 */
@Component
public class WebhookHttpClient implements DisposableBean {

    static final long TIMEOUT_MS = 10_000;
    private static final int RESPONSE_PEEK_BYTES = 512;

    private final HttpClient httpClient;
    private final UrlValidator urlValidator;

    public WebhookHttpClient(SafeSocketAddressResolver addressResolver, UrlValidator urlValidator) throws Exception {
        this.urlValidator = urlValidator;
        HttpClient client = new HttpClient();
        client.setFollowRedirects(false);
        client.setSocketAddressResolver(addressResolver);
        client.setUserAgentField(new HttpField(HttpHeader.USER_AGENT, "SEOPulse-Webhooks/1.0"));
        client.setConnectTimeout(TIMEOUT_MS);
        client.setIdleTimeout(TIMEOUT_MS);
        client.setHttpCookieStore(new HttpCookieStore.Empty());
        client.start();
        this.httpClient = client;
    }

    public record Result(int status, String bodyPreview) {
        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
    }

    public Result postJson(String url, String body, Map<String, String> headers) throws IOException, InterruptedException {

        URI uri = requireHttps(url);
        InputStreamResponseListener listener = new InputStreamResponseListener();

        httpClient.newRequest(uri)
                .method(HttpMethod.POST)
                .timeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .headers(fields -> headers.forEach(fields::put))
                .body(new StringRequestContent("application/json", body, StandardCharsets.UTF_8))
                .send(listener);

        Response response;
        try {
            response = listener.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            listener.close();
            throw new SocketTimeoutException("Webhook timed out");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw new IOException(cause.getMessage() == null ? "Webhook request failed" : cause.getMessage(), cause);
        }

        String preview;
        try (InputStream input = listener.getInputStream()) {
            preview = new String(input.readNBytes(RESPONSE_PEEK_BYTES), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            preview = "";
        }
        response.abort(new IOException("Response body not needed"));
        return new Result(response.getStatus(), preview);
    }

    URI requireHttps(String url) {
        URI uri = urlValidator.validateStructure(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Webhook URLs must use HTTPS");
        }
        return uri;
    }

    @Override
    public void destroy() throws Exception {
        httpClient.stop();
    }
}
