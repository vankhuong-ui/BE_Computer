package com.example.MyProject.ORDER.controller;

import com.example.MyProject.ORDER.common.ApiResponse;
import com.example.MyProject.ORDER.common.BaseController;
import com.example.MyProject.ORDER.dto.payment.PaymentResponse;
import com.example.MyProject.ORDER.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController extends BaseController {

    private final PaymentService paymentService;

    @GetMapping("/create-vnpay/{order_id}")
    @PreAuthorize("hasAnyAuthority('USER', 'ROLE_USER')")
    public ApiResponse<Map<String, String>> createPayment(
            @PathVariable("order_id") Long orderId,
            HttpServletRequest request
    ) {
        String paymentUrl = paymentService.createVNPayPayment(orderId, request);
        return successDataResponse(
                "Create VNPay payment URL success!",
                Map.of("paymentUrl", paymentUrl)
        );
    }

    @GetMapping("/vnpay-return")
    public ApiResponse<PaymentResponse> vnpayReturn(
            @RequestParam Map<String, String> queryParams
    ) {
        PaymentResponse response = paymentService.processVNPayReturn(queryParams);
        return successDataResponse(
                "Process VNPay return success!",
                response
        );
    }

    @GetMapping("/order/{order_id}/history")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<List<PaymentResponse>> getPaymentsByOrder(
            @PathVariable("order_id") Long orderId
    ) {
        return successDataResponse(
                "Get payment history success!",
                paymentService.getPaymentsByOrderId(orderId)
        );
    }

    @GetMapping("/order/{order_id}/latest")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_USER', 'ROLE_ADMIN')")
    public ApiResponse<PaymentResponse> getLatestPaymentByOrder(
            @PathVariable("order_id") Long orderId
    ) {
        return successDataResponse(
                "Get latest payment detail success!",
                paymentService.getLatestPaymentByOrderId(orderId)
        );
    }
}