package com.example.MyProject.PRODUCT.mapper;

import com.example.MyProject.PRODUCT.dto.inventory.InventoryRequest;
import com.example.MyProject.PRODUCT.dto.inventory.InventoryResponse;
import com.example.MyProject.PRODUCT.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface InventoryMapper {

    @Mapping(target = "products.id", source = "productId")
    @Mapping(target = "branch.id", source = "branchId")
    Inventory toEntity(InventoryRequest inventoryRequest);

    @Mapping(target = "productId", source = "products.id")
    @Mapping(target = "productName", source = "products.nameProduct")
    @Mapping(target = "branchId", source = "branch.id")
    @Mapping(target = "branchCode", source = "branch.code")
    @Mapping(target = "branchName", source = "branch.name")
    @Mapping(target = "branchCity", source = "branch.city")
    @Mapping(target = "branchAddress", source = "branch.address")
    @Mapping(target = "branchPhone", source = "branch.phone")
    InventoryResponse toResponse(Inventory inventory);
}
