package com.example.MyProject.PRODUCT.dto.product.response;

import java.math.BigDecimal;

public record ResponseProduct(
        Long id,
        String nameProduct,
        String slug,
        BigDecimal price,
        String specs,
        String image,
        String description,
        Long brandsId,
        Long categoriesId,
        String brandsName,
        String categoriesName,
        String groupType,
        Integer totalStock,
        Integer locationStock,
        Integer availableBranchCount
) {
}
