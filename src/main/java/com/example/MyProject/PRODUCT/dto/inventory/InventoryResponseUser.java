package com.example.MyProject.PRODUCT.dto.inventory;

public record InventoryResponseUser(
        Long id,
        Long productId,
        String productName,
        int soluong
) {
}
