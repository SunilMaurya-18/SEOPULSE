package com.seopulse.auth.service;

import com.seopulse.auth.config.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Have I Been Pwned range API (k-anonymity): only the first five hex
 * characters of the password's SHA-1 hash leave the server. Fails open,
 * because an outage of a third-party service must not block sign-ups.
 */
@Component
@Slf4j
public class HibpBreachedPasswordChecker implements BreachedPasswordChecker {

    private final AuthProperties properties;
    private final HttpClient httpClient;

    public HibpBreachedPasswordChecker(AuthProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getBreachedPasswordTimeout())
                .build();
    }

    @Override
    public boolean isBreached(String password) {

        if (!properties.isBreachedPasswordCheck()) {
            return false;
        }

        String hash = sha1Hex(password);
        String prefix = hash.substring(0, 5);
        String suffix = hash.substring(5);

        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getBreachedPasswordApiUrl() + prefix))
                .timeout(properties.getBreachedPasswordTimeout())
                .header("Add-Padding", "true")
                .header("User-Agent", "SEOPulse-password-check")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Breached-password check unavailable: status={}", response.statusCode());
                return false;
            }

            return response.body().lines().anyMatch(line -> {
                int colon = line.indexOf(':');
                return colon > 0
                        && line.substring(0, colon).equalsIgnoreCase(suffix)
                        && !line.substring(colon + 1).trim().equals("0");
            });
        } catch (IOException ex) {
            log.warn("Breached-password check failed: {}", ex.getMessage());
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String sha1Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().withUpperCase().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 is not available", ex);
        }
    }
}
