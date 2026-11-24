package com.example.MyProject.REVIEW.dto.warranty;

import java.time.LocalDateTime;

public record WarrantyCardResponse(
        Long orderId,
        Long productId,
        String productName,
        String productImage,
        Integer quantity,
        Integer unitNumber,
        String serialCode,
        LocalDateTime purchaseDate,
        LocalDateTime expireDate,
        Integer warrantyMonths,
        long remainingDays,
        String warrantyStatus
) {
}
