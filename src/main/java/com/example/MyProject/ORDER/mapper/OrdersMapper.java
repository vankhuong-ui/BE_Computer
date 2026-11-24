package com.example.MyProject.ORDER.mapper;

import com.example.MyProject.ORDER.dto.orderItem.OrderItemResponse;
import com.example.MyProject.ORDER.dto.orders.OrdersRequest;
import com.example.MyProject.ORDER.dto.orders.OrdersResponse;
import com.example.MyProject.ORDER.entity.OrderItem;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.PRODUCT.mapper.ProductMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        uses = ProductMapper.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface OrdersMapper {

    @Mapping(source = "AddressesId", target = "useraddresses.id")
    @Mapping(target = "orderItems", source = "orderItems")
    @Mapping(target = "paidAt", ignore = true)
    Orders toEntity(OrdersRequest ordersRequest);

    @Mapping(source = "useraddresses.id", target = "AddressesId")
    @Mapping(source = "pickupBranch.id", target = "pickupBranchId")
    @Mapping(source = "pickupBranch.name", target = "pickupBranchName")
    @Mapping(source = "pickupBranch.address", target = "pickupBranchAddress")
    @Mapping(source = "useraddresses.address", target = "shippingAddress")
    @Mapping(source = "orderItems", target = "orderItems")
    OrdersResponse toResponse(Orders orders);

    @Mapping(source = "products", target = "product")
    @Mapping(source = "branch.id", target = "branchId")
    @Mapping(source = "branch.name", target = "branchName")
    @Mapping(source = "branch.city", target = "branchCity")
    OrderItemResponse toOrderItemResponse(OrderItem orderItem);
}
