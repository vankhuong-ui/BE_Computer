package com.example.MyProject.ORDER.dto.payment;

import java.math.BigDecimal;

public record SePayPaymentInfoResponse(
        Long orderId,
        BigDecimal amount,
        String bankName,
        String accountNumber,
        String accountName,
        String transferContent,
        String qrUrl,
        String orderStatus
) {
}
