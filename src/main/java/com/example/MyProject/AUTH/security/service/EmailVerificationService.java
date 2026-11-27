package com.example.MyProject.AUTH.security.service;

import com.example.MyProject.AUTH.service.RedisOtpService;
import com.example.MyProject.common.BrevoEmailClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisOtpService redisOtpService;
    private final BrevoEmailClient brevoEmailClient;

    public void sendOtp(String gmail) {
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        redisOtpService.saveOtp(gmail, otp);

        String subject = "[HK COMPUTER] Mã xác thực tài khoản";
        String textBody = "Mã xác nhận OTP của bạn là: " + otp + "\nMã có hiệu lực trong 5 phút.";

        try {
            brevoEmailClient.sendTextEmail(gmail, subject, textBody);
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
}
