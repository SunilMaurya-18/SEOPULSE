package com.seopulse.billing.razorpay;

import com.seopulse.support.AbstractIntegrationTest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RazorpayIntegrationTest extends AbstractIntegrationTest {

    private static final String KEY_SECRET = "rzp-test-secret";
    private static final String WEBHOOK_SECRET = "rzp-webhook-secret";

    @Autowired
    RazorpayProperties properties;

    private HttpServer razorpay;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private volatile String subscriptionJson = "{}";

    @BeforeEach
    void fakeRazorpay() throws IOException {
        razorpay = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        razorpay.createContext("/v1/subscriptions", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + " " + body);
            byte[] reply = subscriptionJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, reply.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(reply);
            }
        });
        razorpay.start();
        properties.setApiUrl("http://127.0.0.1:" + razorpay.getAddress().getPort() + "/v1");
        properties.setKeyId("rzp_test_key");
        properties.setKeySecret(KEY_SECRET);
        properties.setWebhookSecret(WEBHOOK_SECRET);
        properties.setProMonthlyPlanId("plan_pro_m");
        properties.setAgencyMonthlyPlanId("plan_agency_m");
    }

    @AfterEach
    void restore() {
        RazorpayProperties defaults = new RazorpayProperties();
        properties.setApiUrl(defaults.getApiUrl());
        properties.setKeyId("");
        properties.setKeySecret("");
        properties.setWebhookSecret("");
        properties.setProMonthlyPlanId("");
        properties.setAgencyMonthlyPlanId("");
        razorpay.stop(0);
    }

    @Test
    void checkoutVerifyAndCancellationKeepThePlanInSync() throws Exception {
        TestUser owner = registerVerifiedUser("rzp-owner");
        long orgId = orgId(owner);
        String subscriptionId = "sub_" + UUID.randomUUID().toString().replace("-", "");

        subscriptionJson = "{\"id\": \"" + subscriptionId + "\", \"status\": \"created\"}";
        mvc.perform(authed(post("/api/v1/orgs/{id}/billing/razorpay/subscription", orgId), owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\": \"PRO\", \"interval\": \"month\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyId").value("rzp_test_key"))
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId))
                .andExpect(jsonPath("$.email").value(owner.email()));
        assertThat(requests).anyMatch(r -> r.startsWith("POST /v1/subscriptions ")
                && r.contains("\"plan_id\":\"plan_pro_m\"") && r.contains("\"organizationId\":\"" + orgId + "\""));

        String paymentId = "pay_123";
        mvc.perform(authed(post("/api/v1/orgs/{id}/billing/razorpay/verify", orgId), owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody(paymentId, subscriptionId, "0".repeat(64))))
                .andExpect(status().isBadRequest());

        subscriptionJson = subscription(subscriptionId, "active", orgId, "plan_pro_m");
        mvc.perform(authed(post("/api/v1/orgs/{id}/billing/razorpay/verify", orgId), owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyBody(paymentId, subscriptionId, hmac(paymentId + "|" + subscriptionId, KEY_SECRET))))
                .andExpect(status().isNoContent());

        mvc.perform(authed(get("/api/v1/orgs/{id}/billing", orgId), owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("PRO"))
                .andExpect(jsonPath("$.provider").value("RAZORPAY"))
                .andExpect(jsonPath("$.razorpayAvailable").value(true))
                .andExpect(jsonPath("$.currentPeriodEnd").exists());

        mvc.perform(authed(post("/api/v1/orgs/{id}/billing/razorpay/subscription", orgId), owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\": \"AGENCY\", \"interval\": \"month\"}"))
                .andExpect(status().isConflict());

        String event = """
                {"event": "subscription.cancelled", "payload": {"subscription": {"entity": %s}}}
                """.formatted(subscription(subscriptionId, "cancelled", orgId, "plan_pro_m"));
        mvc.perform(post("/api/v1/billing/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", hmac(event, "wrong-secret"))
                        .content(event))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/billing/razorpay/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", hmac(event, WEBHOOK_SECRET))
                        .header("X-Razorpay-Event-Id", "evt_" + UUID.randomUUID())
                        .content(event))
                .andExpect(status().isOk());

        mvc.perform(authed(get("/api/v1/orgs/{id}/billing", orgId), owner.accessToken()))
                .andExpect(jsonPath("$.planCode").value("FREE"))
                .andExpect(jsonPath("$.provider").doesNotExist());
    }

    @Test
    void onlyOwnersCanStartCheckout() throws Exception {
        TestUser owner = registerVerifiedUser("rzp-owner2");
        TestUser stranger = registerVerifiedUser("rzp-stranger");

        mvc.perform(authed(post("/api/v1/orgs/{id}/billing/razorpay/subscription", orgId(owner)), stranger.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\": \"PRO\", \"interval\": \"month\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(403, 404));
        assertThat(requests).isEmpty();
    }

    private long orgId(TestUser user) throws Exception {
        return JSON.readTree(mvc.perform(authed(get("/api/v1/orgs"), user.accessToken()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString())
                .get(0).get("id").asLong();
    }

    private static String subscription(String id, String status, long orgId, String planId) {
        return """
                {"id": "%s", "status": "%s", "plan_id": "%s", "current_end": 1893456000,
                 "notes": {"organizationId": "%d"}}
                """.formatted(id, status, planId, orgId);
    }

    private static String verifyBody(String paymentId, String subscriptionId, String signature) {
        return "{\"paymentId\": \"%s\", \"subscriptionId\": \"%s\", \"signature\": \"%s\"}"
                .formatted(paymentId, subscriptionId, signature);
    }

    private static String hmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
