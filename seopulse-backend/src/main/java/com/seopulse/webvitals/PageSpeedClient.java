package com.seopulse.webvitals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/** Thin client for the PageSpeed Insights v5 API. */
@Component
public class PageSpeedClient {

    public record Measurement(
            Integer performanceScore,
            Integer labLcpMs,
            BigDecimal labCls,
            Integer labTbtMs,
            Integer labFcpMs,
            Integer labSpeedIndexMs,
            Integer fieldLcpMs,
            BigDecimal fieldCls,
            Integer fieldInpMs,
            String fieldCategory
    ) {
    }

    public static class PageSpeedException extends Exception {
        public PageSpeedException(String message) {
            super(message);
        }
    }

    private final WebVitalsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PageSpeedClient(WebVitalsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Measurement measure(String url) throws PageSpeedException, InterruptedException {
        StringBuilder query = new StringBuilder(properties.getApiUrl())
                .append("?url=").append(encode(url))
                .append("&strategy=").append(encode(properties.getStrategy()))
                .append("&category=performance");
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            query.append("&key=").append(encode(properties.getApiKey()));
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(query.toString()))
                .timeout(properties.getTimeout())
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        JsonNode body;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            body = objectMapper.readTree(response.body());
        } catch (IOException ex) {
            throw new PageSpeedException("PageSpeed Insights could not be reached.");
        }

        if (response.statusCode() == 429) {
            throw new PageSpeedException("PageSpeed Insights quota exceeded. Try again later.");
        }
        if (response.statusCode() != 200) {
            String message = body.path("error").path("message").asText("");
            throw new PageSpeedException(message.contains("FAILED_DOCUMENT_REQUEST") || message.contains("ERRORED_DOCUMENT_REQUEST")
                    ? "Google could not load the homepage to measure it."
                    : "PageSpeed Insights returned HTTP " + response.statusCode() + ".");
        }
        return parse(body);
    }

    static Measurement parse(JsonNode body) {
        JsonNode lighthouse = body.path("lighthouseResult");
        JsonNode audits = lighthouse.path("audits");
        JsonNode scoreNode = lighthouse.path("categories").path("performance").path("score");
        JsonNode field = body.path("loadingExperience").path("metrics");

        BigDecimal fieldCls = null;
        JsonNode clsPercentile = field.path("CUMULATIVE_LAYOUT_SHIFT_SCORE").path("percentile");
        if (clsPercentile.isNumber()) {
            fieldCls = BigDecimal.valueOf(clsPercentile.asDouble() / 100).setScale(3, RoundingMode.HALF_UP);
        }
        String category = body.path("loadingExperience").path("overall_category").asText(null);

        return new Measurement(
                scoreNode.isNumber() ? (int) Math.round(scoreNode.asDouble() * 100) : null,
                millis(audits.path("largest-contentful-paint")),
                decimal(audits.path("cumulative-layout-shift")),
                millis(audits.path("total-blocking-time")),
                millis(audits.path("first-contentful-paint")),
                millis(audits.path("speed-index")),
                percentile(field.path("LARGEST_CONTENTFUL_PAINT_MS")),
                fieldCls,
                percentile(field.path("INTERACTION_TO_NEXT_PAINT")),
                category
        );
    }

    private static Integer millis(JsonNode audit) {
        JsonNode value = audit.path("numericValue");
        return value.isNumber() ? (int) Math.round(value.asDouble()) : null;
    }

    private static BigDecimal decimal(JsonNode audit) {
        JsonNode value = audit.path("numericValue");
        return value.isNumber() ? BigDecimal.valueOf(value.asDouble()).setScale(3, RoundingMode.HALF_UP) : null;
    }

    private static Integer percentile(JsonNode metric) {
        JsonNode value = metric.path("percentile");
        return value.isNumber() ? value.asInt() : null;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
