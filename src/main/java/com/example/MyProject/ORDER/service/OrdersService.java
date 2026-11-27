package com.example.MyProject.ORDER.service;

import com.example.MyProject.ORDER.dto.orders.OrdersRequest;
import com.example.MyProject.ORDER.dto.orders.OrdersResponse;
import com.example.MyProject.ORDER.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrdersService {
    void createOrser(OrdersRequest ordersRequest);
    void updateOrderAddresses(Long id, Long addressesid);
    Page<OrdersResponse> getAllOrders(Pageable pageable);
    List<OrdersResponse> showOrders();
    void updateStatus(Long id, String status);
    OrdersResponse searchOrder(Long id);
    void cancelledOrder(Long id);

    List<Orders> findOrdersWithDetailsByAccount(Long accountId);
    Optional<Orders> findOrderForWarrantyLookup(Long orderId, Long accountId, boolean isAdmin);
    Orders requireOrderForWarrantyClaim(Long orderId, Long accountId);
    Orders requireOwnedOrderWithDetails(Long orderId, Long accountId);
}
