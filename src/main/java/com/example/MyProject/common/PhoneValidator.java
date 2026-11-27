package com.example.MyProject.common;

import com.example.MyProject.common.exception.BusinessException;

import java.util.regex.Pattern;

public final class PhoneValidator {

    public static final String PHONE_REGEX = "^0\\d{9}$";
    private static final Pattern VN_PHONE_PATTERN = Pattern.compile(PHONE_REGEX);

    private PhoneValidator() {
    }

    public static String validateAndNormalize(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new BusinessException("Vui lòng nhập số điện thoại liên hệ");
        }
        String digits = rawPhone.replaceAll("[\\s.\\-]", "").trim();
        if (!VN_PHONE_PATTERN.matcher(digits).matches()) {
            throw new BusinessException("Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0 (ví dụ: 0912345678)");
        }
        return digits;
    }
}
