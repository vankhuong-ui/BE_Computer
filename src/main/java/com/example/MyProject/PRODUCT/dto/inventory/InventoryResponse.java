package com.example.MyProject.PRODUCT.dto.inventory;

public record InventoryResponse(
        Long id,
        Long productId,
        String productName,
        Long branchId,
        String branchCode,
        String branchName,
        String branchCity,
        String branchAddress,
        String branchPhone,
        int soluong,
        int selled
) {
}
