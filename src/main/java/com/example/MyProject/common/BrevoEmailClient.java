package com.example.MyProject.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

/**
 * Shared HTTP email client for Brevo Transactional SMTP API v3.
 * Reuses a single JDK HttpClient instance across OTP and Order Notification flows.
 */
@Slf4j
@Component
public class BrevoEmailClient {

    private static final URI BREVO_URI = URI.create("https://api.brevo.com/v3/smtp/email");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${brevo.api.key:${BREVO_API_KEY:}}")
    private String brevoApiKey = "";

    @Value("${brevo.from.email:${BREVO_FROM_EMAIL:}}")
    private String brevoFromEmail = "";

    public boolean isConfigured() {
        if (brevoApiKey == null || brevoApiKey.isBlank() || brevoFromEmail == null || brevoFromEmail.isBlank()) {
            return false;
        }
        String keyLower = brevoApiKey.trim().toLowerCase(Locale.ROOT);
        return !keyLower.startsWith("dummy") && !keyLower.equals("changeme");
    }

    public void sendTextEmail(String toEmail, String subject, String textBody) {
        if (!isConfigured()) {
            throw new IllegalStateException("Chưa cấu hình BREVO_API_KEY hoặc BREVO_FROM_EMAIL hợp lệ");
        }

        String jsonPayload = """
                {
                  "sender": { "email": "%s", "name": "HK COMPUTER" },
                  "to": [ { "email": "%s" } ],
                  "subject": "%s",
                  "textContent": "%s"
                }
                """.formatted(
                escapeJson(brevoFromEmail.trim()),
                escapeJson(toEmail.trim()),
                escapeJson(subject),
                escapeJson(textBody)
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(BREVO_URI)
                    .timeout(Duration.ofSeconds(25))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Brevo API lỗi HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi kết nối Brevo API: " + e.getMessage(), e);
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
