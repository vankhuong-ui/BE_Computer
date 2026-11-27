package com.example.MyProject.ORDER.controller;

import com.example.MyProject.common.ApiResponse;
import com.example.MyProject.common.BaseController;
import com.example.MyProject.ORDER.dto.orders.OrdersRequest;
import com.example.MyProject.ORDER.dto.orders.OrdersResponse;
import com.example.MyProject.ORDER.service.OrdersService;
import com.example.MyProject.common.ProcessPageable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController extends BaseController {

    private final OrdersService ordersService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<String> createOrder(
            @RequestBody OrdersRequest request
    ) {
        ordersService.createOrser(request);
        return successResponse("Create order success!");
    }

    @PutMapping("/{id}/update-address")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<String> updateOrderAddresses(
            @PathVariable("id") Long id,
            @RequestParam("address_id") Long addressId
    ) {
        ordersService.updateOrderAddresses(id, addressId);
        return successResponse("Update order address success!");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Page<OrdersResponse>> getAllOrders(
            @RequestParam(name = "page_no", defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable =
                ProcessPageable.createPageable(
                        page, size, sortBy, sortDir
                );

        return successDataResponse(
                "Get all orders success",
                ordersService.getAllOrders(pageable)
        );
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<List<OrdersResponse>> showMyOrders() {
        return successDataResponse(
                "Get my orders success",
                ordersService.showOrders()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<OrdersResponse> searchOrder(
            @PathVariable("id") Long id
    ) {
        return successDataResponse(
                "Get order detail success",
                ordersService.searchOrder(id)
        );
    }

    @PutMapping("/{id}/update-status")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<String> updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status
    ) {
        ordersService.updateStatus(id, status);
        return successResponse("Update order status success!");
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<String> cancelledOrder(
            @PathVariable("id") Long id
    ) {
        ordersService.cancelledOrder(id);
        return successResponse("Cancel order success!");
    }
}