package com.example.MyProject.ORDER.dto.orders;

import com.example.MyProject.ORDER.dto.orderItem.OrderItemRequest;

import java.util.List;

public record OrdersRequest(
     Long AddressesId,
     String fulfillmentType,
     Long pickupBranchId,
     Long preferredBranchId,
     String preferredCity,
     String receiverName,
     String receiverPhone,
     String voucherCode,
     List<OrderItemRequest> orderItems
) {
}
