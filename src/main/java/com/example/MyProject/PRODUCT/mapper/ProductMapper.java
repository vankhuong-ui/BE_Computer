package com.example.MyProject.PRODUCT.mapper;

import com.example.MyProject.PRODUCT.dto.product.request.RequestProduct;
import com.example.MyProject.PRODUCT.dto.product.response.ResponseProduct;
import com.example.MyProject.PRODUCT.entity.Products;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {
    @Mapping(source ="brandsId",target = "brands.id")
    @Mapping(source="categoriesId",target ="categories.id")
    Products toEntity(RequestProduct requestProduct);
    @Mapping(source = "brands.id", target = "brandsId")
    @Mapping(source = "categories.id", target = "categoriesId")
    ResponseProduct toReponse(Products products);
    void updateProduct(RequestProduct requestProduct,@MappingTarget Products products);
}
