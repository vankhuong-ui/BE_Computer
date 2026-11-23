package com.example.MyProject.REVIEW.dto.repair;

import com.example.MyProject.AUTH.common.PhoneValidator;
import jakarta.validation.constraints.Pattern;

public record RepairBookingRequest(
        String customerName,
        @Pattern(regexp = PhoneValidator.PHONE_REGEX, message = "Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0")
        String customerPhone,
        String customerEmail,
        String serviceMethod,
        String branchName,
        String homeAddress,
        String deviceModel,
        String serviceType,
        String issueDescription,
        String appointmentDate,
        String appointmentTime
) {
}
