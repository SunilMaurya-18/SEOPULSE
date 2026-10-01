package com.seopulse.report;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Short-lived download links for stored PDFs:
 * {@code /api/v1/public/report-files/{id}?expires=<unix>&sig=<hmac>}.
 */
@Component
public class ReportUrlSigner {

    private final byte[] key;
    private final ReportProperties properties;

    public ReportUrlSigner(ReportProperties properties, Environment environment) {
        this.properties = properties;
        String configured = properties.getSigningKey();
        if (configured == null || configured.isBlank()) {
            // Derive a separate key from the JWT secret so the two never collide.
            configured = "report-download:" + environment.getProperty("jwt.secret", "");
        }
        this.key = configured.getBytes(StandardCharsets.UTF_8);
    }

    public record SignedUrl(String url, Instant expiresAt) {
    }

    public SignedUrl sign(Long reportId, Instant now) {
        Instant expiresAt = now.plus(properties.getDownloadUrlTtl());
        long expires = expiresAt.getEpochSecond();
        String url = "/api/v1/public/report-files/" + reportId + "?expires=" + expires + "&sig=" + signature(reportId, expires);
        return new SignedUrl(url, expiresAt);
    }

    public boolean verify(Long reportId, long expires, String signature, Instant now) {
        if (signature == null || expires < now.getEpochSecond()) {
            return false;
        }
        return MessageDigest.isEqual(
                signature(reportId, expires).getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private String signature(Long reportId, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((reportId + ":" + expires).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HmacSHA256 is not available", ex);
        }
    }
}
