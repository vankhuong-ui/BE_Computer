package com.example.MyProject.PRODUCT.dto.inventory;

public record InventoryTransferRequest(
        Long productId,
        Long fromBranchId,
        Long toBranchId,
        int quantity
) {
}
