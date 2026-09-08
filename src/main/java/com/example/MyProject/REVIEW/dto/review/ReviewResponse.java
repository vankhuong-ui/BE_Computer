package com.example.MyProject.REVIEW.dto.review;


import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long orderId,
        Long productId,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {}