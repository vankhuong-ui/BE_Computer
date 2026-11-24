package com.example.MyProject.ORDER.dto.voucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherRequest(
        String code,
        String title,
        String description,
        String discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscount,
        BigDecimal minOrderAmount,
        Boolean firstOrderOnly,
        Integer usageLimit,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Boolean active
) {
}
