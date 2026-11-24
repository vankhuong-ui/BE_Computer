package com.example.MyProject.PRODUCT.dto.branch;

public record BranchRequest(
        String code,
        String name,
        String city,
        String district,
        String address,
        String phone,
        String openingHours,
        String services,
        Boolean active
) {
}
