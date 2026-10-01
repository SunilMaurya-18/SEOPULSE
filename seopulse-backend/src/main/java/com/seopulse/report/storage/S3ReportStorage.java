package com.seopulse.report.storage;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.FileNotFoundException;
import java.io.IOException;

public class S3ReportStorage implements ReportStorage, AutoCloseable {

    private final S3Client client;
    private final String bucket;

    public S3ReportStorage(S3Client client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, byte[] content) throws IOException {
        try {
            client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType("application/pdf")
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (SdkException ex) {
            throw new IOException("Could not store report: " + ex.getMessage(), ex);
        }
    }

    @Override
    public byte[] get(String key) throws IOException {
        try {
            return client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
        } catch (NoSuchKeyException ex) {
            throw new FileNotFoundException(key);
        } catch (SdkException ex) {
            throw new IOException("Could not read report: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(String key) throws IOException {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException ex) {
            throw new IOException("Could not delete report: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
