package com.seopulse.report;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "seopulse.reports")
@Getter
@Setter
public class ReportProperties {

    /** Directory for PDFs when no S3 bucket is configured. API and worker must share it. */
    private String storageDir = "./data/reports";

    /** When set, PDFs are stored in this S3 (or S3-compatible) bucket instead of on disk. */
    private String s3Bucket = "";
    private String s3Region = "us-east-1";
    /** Custom endpoint for S3-compatible stores such as MinIO or R2; blank for AWS. */
    private String s3Endpoint = "";
    private boolean s3PathStyle = false;

    /** HMAC key for signed download URLs. */
    private String signingKey = "";

    /** How long a signed download URL stays valid. */
    private Duration downloadUrlTtl = Duration.ofMinutes(10);

    private int defaultShareDays = 30;
    private int maxShareDays = 365;

    private int maxAttempts = 3;

    public boolean s3Enabled() {
        return s3Bucket != null && !s3Bucket.isBlank();
    }
}
