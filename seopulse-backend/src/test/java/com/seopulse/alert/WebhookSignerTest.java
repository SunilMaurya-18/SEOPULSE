package com.seopulse.alert;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignerTest {

    private static final String SECRET = "whsec_test";
    private static final String BODY = "{\"type\":\"SCORE_DROP\"}";

    @Test
    void signatureIsHmacOfTimestampAndBody() throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = HexFormat.of().formatHex(mac.doFinal(("1700000000." + BODY).getBytes(StandardCharsets.UTF_8)));

        assertThat(WebhookSigner.sign(SECRET, 1_700_000_000L, BODY)).isEqualTo("t=1700000000,v1=" + expected);
    }

    @Test
    void verifyAcceptsFreshSignatures() {
        String header = WebhookSigner.sign(SECRET, 1_700_000_000L, BODY);

        assertThat(WebhookSigner.verify(SECRET, header, BODY, 1_700_000_100L, 300)).isTrue();
    }

    @Test
    void verifyRejectsTamperingWrongSecretAndOldTimestamps() {
        String header = WebhookSigner.sign(SECRET, 1_700_000_000L, BODY);

        assertThat(WebhookSigner.verify(SECRET, header, BODY + " ", 1_700_000_000L, 300)).isFalse();
        assertThat(WebhookSigner.verify("other", header, BODY, 1_700_000_000L, 300)).isFalse();
        assertThat(WebhookSigner.verify(SECRET, header, BODY, 1_700_001_000L, 300)).isFalse();
        assertThat(WebhookSigner.verify(SECRET, null, BODY, 1_700_000_000L, 300)).isFalse();
        assertThat(WebhookSigner.verify(SECRET, "t=abc,v1=00", BODY, 1_700_000_000L, 300)).isFalse();
    }

    @Test
    void retryBackoffGrowsAndCapsAtSixHours() {
        assertThat(AlertDispatcher.backoff(1)).hasMinutes(1);
        assertThat(AlertDispatcher.backoff(2)).hasMinutes(5);
        assertThat(AlertDispatcher.backoff(3)).hasMinutes(15);
        assertThat(AlertDispatcher.backoff(4)).hasHours(1);
        assertThat(AlertDispatcher.backoff(5)).hasHours(6);
        assertThat(AlertDispatcher.backoff(9)).hasHours(6);
    }
}
