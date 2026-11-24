package com.example.MyProject.ORDER.dto.orderItem;

import com.example.MyProject.PRODUCT.dto.product.response.ResponseProduct;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Integer quantity,
        BigDecimal price,
        Long branchId,
        String branchName,
        String branchCity,
        ResponseProduct product
) {
}
