package com.seopulse.webvitals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageSpeedClientTest {

    private HttpServer server;
    private final AtomicReference<String> lastQuery = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String reply = PageSpeedSamples.WITH_FIELD_DATA;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/psi", exchange -> {
            lastQuery.set(exchange.getRequestURI().getRawQuery());
            byte[] body = reply.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void readsLabAndFieldMetrics() throws Exception {
        PageSpeedClient.Measurement m = client("secret-key").measure("https://example.com/");

        assertThat(lastQuery.get())
                .contains("url=https%3A%2F%2Fexample.com%2F", "strategy=mobile", "category=performance", "key=secret-key");
        assertThat(m.performanceScore()).isEqualTo(73);
        assertThat(m.labLcpMs()).isEqualTo(3120);
        assertThat(m.labCls()).isEqualByComparingTo(new BigDecimal("0.041"));
        assertThat(m.labTbtMs()).isEqualTo(250);
        assertThat(m.labFcpMs()).isEqualTo(1401);
        assertThat(m.labSpeedIndexMs()).isEqualTo(2800);
        assertThat(m.fieldLcpMs()).isEqualTo(2900);
        assertThat(m.fieldCls()).isEqualByComparingTo(new BigDecimal("0.05"));
        assertThat(m.fieldInpMs()).isEqualTo(180);
        assertThat(m.fieldCategory()).isEqualTo("AVERAGE");
    }

    @Test
    void leavesFieldDataEmptyForLowTrafficSites() throws Exception {
        reply = """
                {"lighthouseResult": {"categories": {"performance": {"score": 1}}, "audits": {}}}
                """;

        PageSpeedClient.Measurement m = client("").measure("https://example.com/");

        assertThat(lastQuery.get()).doesNotContain("key=");
        assertThat(m.performanceScore()).isEqualTo(100);
        assertThat(m.labLcpMs()).isNull();
        assertThat(m.fieldLcpMs()).isNull();
        assertThat(m.fieldCategory()).isNull();
    }

    @Test
    void explainsQuotaAndUnreachablePages() {
        status = 429;
        reply = "{}";
        assertThatThrownBy(() -> client("").measure("https://example.com/"))
                .hasMessageContaining("quota");

        status = 500;
        reply = """
                {"error": {"code": 500, "message": "Lighthouse returned error: FAILED_DOCUMENT_REQUEST."}}
                """;
        assertThatThrownBy(() -> client("").measure("https://example.com/"))
                .hasMessageContaining("could not load the homepage");
    }

    private PageSpeedClient client(String apiKey) {
        WebVitalsProperties properties = new WebVitalsProperties();
        properties.setApiKey(apiKey);
        properties.setApiUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/psi");
        return new PageSpeedClient(properties, new ObjectMapper());
    }
}
