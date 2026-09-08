package com.example.MyProject.ORDER.controller;

import com.example.MyProject.ORDER.common.ApiResponse;
import com.example.MyProject.ORDER.common.BaseController;
import com.example.MyProject.ORDER.dto.payment.PaymentResponse;
import com.example.MyProject.ORDER.dto.payment.VerifyPaymentRequest;
import com.example.MyProject.ORDER.service.PaymentService;
import com.example.MyProject.PRODUCT.common.ProcessPageable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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

    // ✅ API frontend gọi để xác thực và cập nhật DB
    @PostMapping("/verify")
    public ApiResponse<PaymentResponse> verifyPayment(@RequestBody VerifyPaymentRequest request) {
        PaymentResponse response = paymentService.verifyAndProcess(request);
        return successDataResponse("Verify payment success!", response);
    }

    @GetMapping("/order/history")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Page<PaymentResponse>> getPaymentsByOrder(
            @RequestParam(name = "page_no", defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = ProcessPageable.createPageable(
                page,
                size,
                sortBy,
                sortDir
        );

        return successDataResponse(
                "Get payment history success!",
                paymentService.getPaymentsByOrderId(pageable)
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