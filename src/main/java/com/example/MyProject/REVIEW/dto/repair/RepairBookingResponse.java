package com.example.MyProject.REVIEW.dto.repair;

import java.time.LocalDateTime;

public record RepairBookingResponse(
        Long id,
        Long accountId,
        String customerName,
        String customerPhone,
        String customerEmail,
        String serviceMethod,
        String branchName,
        String homeAddress,
        String deviceModel,
        String serviceType,
        String issueDescription,
        String appointmentDate,
        String appointmentTime,
        String status,
        String adminNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
