package com.seopulse.abuse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaptchaVerifierTest {

    private HttpServer server;
    private final AtomicReference<String> lastForm = new AtomicReference<>();
    private volatile String reply = "{\"success\": true}";
    private volatile int status = 200;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/siteverify", exchange -> {
            lastForm.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
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
    void isSkippedWithoutASecretKey() {
        CaptchaVerifier verifier = new CaptchaVerifier(new AbuseProperties(), new ObjectMapper());

        assertThat(verifier.isEnabled()).isFalse();
        assertThatCode(() -> verifier.verify(null, "1.2.3.4")).doesNotThrowAnyException();
    }

    @Test
    void acceptsTokensCloudflareApproves() {
        CaptchaVerifier verifier = verifier();

        assertThatCode(() -> verifier.verify("good-token", "1.2.3.4")).doesNotThrowAnyException();
        assertThat(lastForm.get()).contains("secret=test-secret", "response=good-token", "remoteip=1.2.3.4");
    }

    @Test
    void rejectsMissingAndRefusedTokens() {
        CaptchaVerifier verifier = verifier();
        assertThatThrownBy(() -> verifier.verify(" ", null)).isInstanceOf(IllegalArgumentException.class);

        reply = "{\"success\": false, \"error-codes\": [\"invalid-input-response\"]}";
        assertThatThrownBy(() -> verifier.verify("bad-token", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("security check failed");
    }

    @Test
    void allowsRequestsWhenCloudflareIsDown() {
        status = 503;
        reply = "unavailable";

        assertThatCode(() -> verifier().verify("any-token", null)).doesNotThrowAnyException();
    }

    private CaptchaVerifier verifier() {
        AbuseProperties properties = new AbuseProperties();
        properties.setTurnstileSecretKey("test-secret");
        properties.setTurnstileVerifyUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify");
        return new CaptchaVerifier(properties, new ObjectMapper());
    }
}
