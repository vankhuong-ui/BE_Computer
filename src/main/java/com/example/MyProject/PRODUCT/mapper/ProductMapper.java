package com.example.MyProject.PRODUCT.mapper;

import com.example.MyProject.PRODUCT.dto.product.request.RequestProduct;
import com.example.MyProject.PRODUCT.dto.product.response.ResponseProduct;
import com.example.MyProject.PRODUCT.entity.Products;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

    @Mapping(source = "brandsId", target = "brands.id")
    @Mapping(source = "categoriesId", target = "categories.id")
    Products toEntity(RequestProduct requestProduct);

    @Mapping(source = "brands.id", target = "brandsId")
    @Mapping(source = "categories.id", target = "categoriesId")
    @Mapping(source = "brands.nameBrands", target = "brandsName")
    @Mapping(source = "categories.nameCategories", target = "categoriesName")
    @Mapping(source = "categories.groupType", target = "groupType")
    ResponseProduct toReponse(Products products);

    @Mapping(target = "brands", ignore = true)
    @Mapping(target = "categories", ignore = true)
    void updateProduct(RequestProduct requestProduct, @MappingTarget Products products);
}
