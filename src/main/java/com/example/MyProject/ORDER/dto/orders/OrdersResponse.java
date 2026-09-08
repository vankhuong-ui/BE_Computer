package com.example.MyProject.ORDER.dto.orders;

import com.example.MyProject.ORDER.dto.orderItem.OrderItemResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdersResponse(
        Long id,
        Long AddressesId,
        BigDecimal totalAmount,
        String status,
        LocalDateTime createdAt,
        List<OrderItemResponse> orderItems
) {

}
