package com.example.MyProject.REVIEW.dto.warranty;

public record WarrantyClaimRequest(
        Long orderId,
        Long productId,
        String issueType,
        String description,
        String contactPhone
) {
}
