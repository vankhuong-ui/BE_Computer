package com.example.MyProject.ORDER.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        String transactionId,
        String paymentGateway,
        BigDecimal amount,
        String status,
        String responseCode,
        LocalDateTime createdAt
) {
}
