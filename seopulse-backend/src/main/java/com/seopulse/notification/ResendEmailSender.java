package com.seopulse.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seopulse.common.email.EmailMessage;
import com.seopulse.common.email.EmailSender;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class ResendEmailSender implements EmailSender {

    private final String apiKey;
    private final String from;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public ResendEmailSender(String apiKey, String from, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.from = from;
        this.objectMapper = objectMapper;
    }

    @Override
    public void send(EmailMessage message) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("from", from);
            payload.put("to", new String[]{message.to()});
            payload.put("subject", message.subject());
            if (message.html() != null && !message.html().isBlank()) {
                payload.put("html", message.html());
            }
            payload.put("text", message.body());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Resend rejected the message: " + response.statusCode());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Email send interrupted", ex);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to send email", ex);
        }
    }
}
