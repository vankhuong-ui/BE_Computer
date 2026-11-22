package com.example.MyProject.REVIEW.dto.repair;

public record RepairBookingRequest(
        String customerName,
        String customerPhone,
        String customerEmail,
        String branchName,
        String deviceModel,
        String serviceType,
        String issueDescription,
        String appointmentDate,
        String appointmentTime
) {
}
