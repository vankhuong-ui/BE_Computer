package com.example.MyProject.PRODUCT.dto.inventory;

public record InventoryWebSocketResponse(
        Long inventoryId,
        Long productId,
        Integer stockQuantity
) {
}