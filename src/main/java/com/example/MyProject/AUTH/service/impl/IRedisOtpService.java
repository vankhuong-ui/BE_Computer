package com.example.MyProject.AUTH.service.impl;


import com.example.MyProject.AUTH.service.RedisOtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IRedisOtpService implements RedisOtpService {

    private final StringRedisTemplate redisTemplate;

    private String getOtpKey(String gmail) {
        return "otp:" + gmail;
    }

    @Override
    public void saveOtp(String gmail, String otp) {

        redisTemplate.opsForValue().set(
                getOtpKey(gmail),
                otp,
                Duration.ofMinutes(5)
        );
    }

    @Override
    public String getOtp(String gmail) {

        return redisTemplate.opsForValue()
                .get(getOtpKey(gmail));
    }

    @Override
    public void deleteOtp(String gmail) {

        redisTemplate.delete(getOtpKey(gmail));
    }
}