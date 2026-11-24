package com.example.MyProject.PRODUCT.dto.branch;

public record BranchResponse(
        Long id,
        String code,
        String name,
        String city,
        String district,
        String address,
        String phone,
        String openingHours,
        String services,
        boolean active,
        long productCount,
        long totalStock
) {
}
