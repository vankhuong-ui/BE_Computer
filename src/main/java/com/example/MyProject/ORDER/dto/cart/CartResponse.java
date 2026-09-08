package com.example.MyProject.ORDER.dto.cart;

import com.example.MyProject.PRODUCT.dto.product.response.ResponseProduct;
import com.example.MyProject.PRODUCT.entity.Products;

public record CartResponse(
        Long id,
        ResponseProduct products,
        Integer quality
) {
}
