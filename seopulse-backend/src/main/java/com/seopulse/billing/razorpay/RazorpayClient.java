package com.seopulse.billing.razorpay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seopulse.common.exception.InvalidStateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/** Minimal client for the Razorpay Subscriptions REST API. */
@Component
@Slf4j
public class RazorpayClient {

    private final RazorpayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public RazorpayClient(RazorpayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public JsonNode createSubscription(String planId, int totalCount, Map<String, String> notes) {
        Map<String, Object> body = Map.of(
                "plan_id", planId,
                "total_count", totalCount,
                "customer_notify", 1,
                "notes", notes);
        return send("POST", "/subscriptions", body);
    }

    public JsonNode fetchSubscription(String subscriptionId) {
        return send("GET", "/subscriptions/" + encode(subscriptionId), null);
    }

    public JsonNode cancelSubscription(String subscriptionId, boolean atCycleEnd) {
        return send("POST", "/subscriptions/" + encode(subscriptionId) + "/cancel",
                Map.of("cancel_at_cycle_end", atCycleEnd ? 1 : 0));
    }

    private JsonNode send(String method, String path, Object body) {
        String credentials = properties.getKeyId() + ":" + properties.getKeySecret();
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(properties.getApiUrl() + path))
                .timeout(properties.getTimeout())
                .header("Authorization", "Basic " + Base64.getEncoder()
                        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)))
                .header("Accept", "application/json");
        try {
            if (body == null) {
                request.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                request.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            }
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() / 100 != 2) {
                log.warn("Razorpay {} {} failed: status={}, error={}", method, path, response.statusCode(),
                        json.path("error").path("description").asText(""));
                throw new InvalidStateException("Razorpay rejected the request. Please try again later.");
            }
            return json;
        } catch (IOException ex) {
            log.warn("Razorpay {} {} unreachable: {}", method, path, ex.getMessage());
            throw new InvalidStateException("Razorpay could not be reached. Please try again.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InvalidStateException("Request interrupted");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
