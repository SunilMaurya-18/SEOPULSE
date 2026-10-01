package com.seopulse.report.storage;

import com.seopulse.report.ReportProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;
import java.nio.file.Path;

@Configuration
@Slf4j
public class ReportStorageConfig {

    @Bean
    public ReportStorage reportStorage(ReportProperties properties) {
        if (!properties.s3Enabled()) {
            log.info("Report storage: filesystem at {}", Path.of(properties.getStorageDir()).toAbsolutePath());
            return new FilesystemReportStorage(Path.of(properties.getStorageDir()));
        }

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getS3Region()))
                .httpClient(UrlConnectionHttpClient.create())
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .forcePathStyle(properties.isS3PathStyle());
        if (properties.getS3Endpoint() != null && !properties.getS3Endpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getS3Endpoint()));
        }
        log.info("Report storage: S3 bucket {}", properties.getS3Bucket());
        return new S3ReportStorage(builder.build(), properties.getS3Bucket());
    }
}
