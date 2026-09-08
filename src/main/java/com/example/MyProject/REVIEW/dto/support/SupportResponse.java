package com.example.MyProject.REVIEW.dto.support;
import java.time.LocalDateTime;

public record SupportResponse(
        Long id,
        Long accountId,
        String username,
        String subject,
        String message,
        String status,
        LocalDateTime createdAt
) {}