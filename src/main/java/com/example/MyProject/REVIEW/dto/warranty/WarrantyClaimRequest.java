package com.example.MyProject.REVIEW.dto.warranty;

import com.example.MyProject.AUTH.common.PhoneValidator;
import jakarta.validation.constraints.Pattern;

public record WarrantyClaimRequest(
        Long orderId,
        Long productId,
        String issueType,
        String description,
        @Pattern(regexp = PhoneValidator.PHONE_REGEX, message = "Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0")
        String contactPhone
) {
}
