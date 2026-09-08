package com.example.MyProject.AUTH.security.service;

import com.example.MyProject.AUTH.service.RedisOtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final JavaMailSender mailSender;

    private final RedisOtpService redisOtpService;


    // =========================
    // GỬI OTP
    // =========================

    public void sendOtp(String gmail) {

        // Tạo OTP 6 số
        String otp = String.valueOf(
                ThreadLocalRandom.current()
                        .nextInt(100000, 1000000)
        );

        // Lưu OTP vào Redis
        // TTL = 5 phút
        redisOtpService.saveOtp(gmail, otp);


        // Tạo email
        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(gmail);

        message.setSubject(
                "Mã xác nhận tài khoản"
        );

        message.setText(
                "Mã xác nhận của bạn là: " + otp +
                        "\nMã có hiệu lực trong 5 phút."
        );

        // Gửi email
        mailSender.send(message);
    }


    // =========================
    // KIỂM TRA OTP
    // =========================

    public boolean verifyOtp(
            String gmail,
            String otp) {

        // Lấy OTP từ Redis
        String savedOtp =
                redisOtpService.getOtp(gmail);

        // Không tồn tại hoặc đã hết hạn
        if (savedOtp == null) {
            return false;
        }

        // OTP sai
        if (!savedOtp.equals(otp)) {
            return false;
        }

        // OTP đúng
        // Xóa ngay khỏi Redis
        redisOtpService.deleteOtp(gmail);

        return true;
    }
}