package com.example.MyProject.ORDER.dto.voucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherResponse(
        Long id,
        String code,
        String title,
        String description,
        String discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscount,
        BigDecimal minOrderAmount,
        boolean firstOrderOnly,
        int usageLimit,
        int usedCount,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean active
) {
}
