package com.example.MyProject.ORDER.service;

import com.example.MyProject.ORDER.dto.orders.OrdersRequest;
import com.example.MyProject.ORDER.dto.orders.OrdersResponse;
import com.example.MyProject.ORDER.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrdersService {
    void createOrser(OrdersRequest request);
    void updateOrderAddresses(Long id,Long addressesid);
    List<OrdersResponse> showOrders();
    void updateStatus(Long id,String status);
    OrdersResponse searchOrder(Long id);
    void cancelledOrder(Long id);
    Page<OrdersResponse> getAllOrders(Pageable pageable);
}
