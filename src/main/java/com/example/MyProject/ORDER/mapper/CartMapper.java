package com.example.MyProject.ORDER.mapper;

import com.example.MyProject.ORDER.dto.cart.CartRequest;
import com.example.MyProject.ORDER.dto.cart.CartResponse;
import com.example.MyProject.ORDER.entity.Cart;
import com.example.MyProject.PRODUCT.mapper.ProductMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = ProductMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {
    @Mapping(source = "accountId", target = "account.id")
    @Mapping(source = "cartRequest.productId", target = "products.id")
    @Mapping(source = "cartRequest.quality", target = "quality")
    Cart toEntity(CartRequest cartRequest, Long accountId);
//     @Mapping(source ="products",target = "products")
    CartResponse toResponse(Cart cart);
}
