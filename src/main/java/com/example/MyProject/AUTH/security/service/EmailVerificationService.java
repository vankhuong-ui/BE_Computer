package com.example.MyProject.AUTH.security.service;

import com.example.MyProject.AUTH.service.RedisOtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisOtpService redisOtpService;

    @Value("${BREVO_API_KEY}")
    private String brevoApiKey;

    @Value("${BREVO_FROM_EMAIL}")
    private String brevoFromEmail;

    public void sendOtp(String gmail) {
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        redisOtpService.saveOtp(gmail, otp);

        String subject = "Mã xác nhận tài khoản";
        String textBody = "Mã xác nhận của bạn là: " + otp + "\nMã có hiệu lực trong 5 phút.";

        try {
            sendEmail(gmail, subject, textBody);
        } catch (Exception e) {
            redisOtpService.deleteOtp(gmail);
            throw new RuntimeException("Không thể gửi email OTP qua Brevo: " + e.getMessage(), e);
        }
    }

    public boolean verifyOtp(String gmail, String otp) {
        String savedOtp = redisOtpService.getOtp(gmail);
        if (savedOtp == null || !savedOtp.equals(otp)) {
            return false;
        }
        redisOtpService.deleteOtp(gmail);
        return true;
    }

    private void sendEmail(String to, String subject, String text) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        // JSON chuẩn của Brevo API v3
        String jsonBody = """
                {
                  "sender": {
                    "email": "%s",
                    "name": "HK COMPUTER"
                  },
                  "to": [
                    {
                      "email": "%s"
                    }
                  ],
                  "subject": "%s",
                  "textContent": "%s"
                }
                """.formatted(
                escapeJson(brevoFromEmail),
                escapeJson(to),
                escapeJson(subject),
                escapeJson(text)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .timeout(Duration.ofSeconds(30))
                .header("api-key", brevoApiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Brevo API lỗi. HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}