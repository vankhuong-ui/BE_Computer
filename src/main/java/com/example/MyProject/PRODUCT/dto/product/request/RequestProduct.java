package com.example.MyProject.PRODUCT.dto.product.request;

import java.math.BigDecimal;

public record RequestProduct(
        String nameProduct,
        String slug,
        BigDecimal price,
        String specs,
        String image,
        String description,
        Long brandsId,
        Long categoriesId,
        Integer initialStock,
        Long initialBranchId,
        Boolean allBranches
) {
}
