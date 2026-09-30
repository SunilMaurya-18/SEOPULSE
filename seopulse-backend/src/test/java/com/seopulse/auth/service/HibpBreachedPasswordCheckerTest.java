package com.seopulse.auth.service;

import com.seopulse.auth.config.AuthProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class HibpBreachedPasswordCheckerTest {

    // SHA-1("password") = 5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8
    private static final String PREFIX = "5BAA6";
    private static final String SUFFIX = "1E4C9B93F3F0682250B6CF8331B7EE68FD8";

    private HttpServer server;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> body = new AtomicReference<>("");
    private final AtomicReference<String> requestedPath = new AtomicReference<>();
    private AuthProperties properties;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/range/", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();

        properties = new AuthProperties();
        properties.setBreachedPasswordApiUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/range/");
        properties.setBreachedPasswordTimeout(Duration.ofSeconds(2));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void detectsBreachedPasswordAndSendsOnlyHashPrefix() {
        body.set("0018A45C4D1DEF81644B54AB7F969B88D65:1\r\n" + SUFFIX + ":3861493\r\n");

        assertThat(new HibpBreachedPasswordChecker(properties).isBreached("password")).isTrue();
        assertThat(requestedPath.get()).isEqualTo("/range/" + PREFIX);
    }

    @Test
    void paddingEntriesWithZeroCountDoNotMatch() {
        body.set(SUFFIX + ":0\r\n");

        assertThat(new HibpBreachedPasswordChecker(properties).isBreached("password")).isFalse();
    }

    @Test
    void unknownPasswordIsAccepted() {
        body.set("0018A45C4D1DEF81644B54AB7F969B88D65:1\r\n");

        assertThat(new HibpBreachedPasswordChecker(properties).isBreached("password")).isFalse();
    }

    @Test
    void failsOpenWhenServiceIsDown() {
        status.set(503);

        assertThat(new HibpBreachedPasswordChecker(properties).isBreached("password")).isFalse();
    }

    @Test
    void disabledCheckMakesNoRequest() {
        properties.setBreachedPasswordCheck(false);

        assertThat(new HibpBreachedPasswordChecker(properties).isBreached("password")).isFalse();
        assertThat(requestedPath.get()).isNull();
    }
}
