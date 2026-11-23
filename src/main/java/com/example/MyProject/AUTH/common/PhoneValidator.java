package com.example.MyProject.AUTH.common;

import java.util.regex.Pattern;

public final class PhoneValidator {

    public static final String PHONE_REGEX = "^0\\d{9}$";
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);

    private PhoneValidator() {
    }

    public static String validateAndNormalize(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Vui lòng nhập số điện thoại liên hệ");
        }
        String normalized = phone.trim();
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new RuntimeException("Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0 (ví dụ: 0912345678)");
        }
        return normalized;
    }
}
