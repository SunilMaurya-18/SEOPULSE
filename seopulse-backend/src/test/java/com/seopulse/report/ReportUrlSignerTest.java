package com.seopulse.report;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReportUrlSignerTest {

    private final ReportUrlSigner signer = new ReportUrlSigner(new ReportProperties(),
            new MockEnvironment().withProperty("jwt.secret", "test-secret"));

    @Test
    void signedUrlsVerifyUntilTheyExpire() {
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        ReportUrlSigner.SignedUrl url = signer.sign(5L, now);
        long expires = url.expiresAt().getEpochSecond();
        String sig = url.url().substring(url.url().indexOf("sig=") + 4);

        assertThat(url.url()).startsWith("/api/v1/public/report-files/5?expires=" + expires);
        assertThat(signer.verify(5L, expires, sig, now)).isTrue();
        assertThat(signer.verify(6L, expires, sig, now)).isFalse();
        assertThat(signer.verify(5L, expires + 1, sig, now)).isFalse();
        assertThat(signer.verify(5L, expires, sig, url.expiresAt().plusSeconds(1))).isFalse();
        assertThat(signer.verify(5L, expires, null, now)).isFalse();
    }
}
