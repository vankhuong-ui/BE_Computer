package com.example.MyProject.ORDER.dto.orders;

import com.example.MyProject.ORDER.dto.orderItem.OrderItemRequest;

import java.util.List;

public record OrdersRequest(
     Long AddressesId,
     List<OrderItemRequest> orderItems
) {
}
