package com.example.MyProject.AUTH.service;
public interface RedisOtpService {

    void saveOtp(String gmail, String otp);

    String getOtp(String gmail);

    void deleteOtp(String gmail);
}