package com.example.MyProject.AUTH.dto.address.request;

import com.example.MyProject.AUTH.common.PhoneValidator;
import jakarta.validation.constraints.Pattern;

public record UseraddressRequest(
        Long account,
        String username,
        @Pattern(regexp = PhoneValidator.PHONE_REGEX, message = "Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0")
        String phone,
        String address,
        String defaultaddress
) {
}
