package com.example.MyProject.PRODUCT.dto.inventory;

public record InventoryRequest(
        Long productId,
        Long branchId,
        int soluong,
        Boolean allBranches
) {
}
