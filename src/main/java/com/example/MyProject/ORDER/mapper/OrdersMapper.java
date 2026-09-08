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

    @Mapping(
            source = "AddressesId",
            target = "useraddresses.id"
    )
    @Mapping(
            target = "orderItems",
            source = "orderItems"
    )
    Orders toEntity(OrdersRequest ordersRequest);


    @Mapping(
            source = "useraddresses.id",
            target = "AddressesId"
    )
    @Mapping(
            source = "orderItems",
            target = "orderItems"
    )
    OrdersResponse toResponse(Orders orders);


    @Mapping(
            source = "products",
            target = "product"
    )
    OrderItemResponse toOrderItemResponse(OrderItem orderItem);
}