package com.seopulse.abuse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Cloudflare Turnstile. Disabled until a secret key is configured. A
 * rejected token fails the request; an unreachable Cloudflare does not,
 * because an outage of a third-party service must not block sign-ups.
 */
@Component
@Slf4j
public class CaptchaVerifier {

    private final AbuseProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CaptchaVerifier(AbuseProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(properties.getTurnstileTimeout()).build();
    }

    public boolean isEnabled() {
        return properties.getTurnstileSecretKey() != null && !properties.getTurnstileSecretKey().isBlank();
    }

    /**
     * @throws IllegalArgumentException if the token is missing or Cloudflare rejects it
     */
    public void verify(String token, String remoteIp) {
        if (!isEnabled()) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Complete the security check and try again.");
        }

        String form = "secret=" + encode(properties.getTurnstileSecretKey())
                + "&response=" + encode(token)
                + (remoteIp != null ? "&remoteip=" + encode(remoteIp) : "");
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getTurnstileVerifyUrl()))
                .timeout(properties.getTurnstileTimeout())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Turnstile unavailable; allowing request: status={}", response.statusCode());
                return;
            }
            JsonNode body = objectMapper.readTree(response.body());
            if (!body.path("success").asBoolean(false)) {
                log.info("Turnstile rejected a token: errors={}", body.path("error-codes"));
                throw new IllegalArgumentException("The security check failed. Refresh the page and try again.");
            }
        } catch (IOException ex) {
            log.warn("Turnstile check failed; allowing request: {}", ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
