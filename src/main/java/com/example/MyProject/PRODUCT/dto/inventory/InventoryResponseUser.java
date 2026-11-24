package com.example.MyProject.PRODUCT.dto.inventory;

import java.util.List;

public record InventoryResponseUser(
        Long id,
        Long productId,
        String productName,
        int soluong,
        int totalStock,
        int availableBranchCount,
        List<InventoryResponse> branches
) {
}
