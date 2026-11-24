package com.example.MyProject.REVIEW.dto.warranty;

import java.time.LocalDateTime;

public record WarrantyClaimResponse(
        Long id,
        Long orderId,
        Long productId,
        String productName,
        String productImage,
        Long accountId,
        String customerEmail,
        Integer unitNumber,
        String serialCode,
        String issueType,
        String description,
        String contactPhone,
        String status,
        String adminNote,
        Integer warrantyMonths,
        LocalDateTime warrantyExpireDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
