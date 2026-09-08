package com.example.MyProject.ORDER.dto.orderItem;

public record OrderItemRequest(
        Long productId,
        Integer quantity
) {
}
