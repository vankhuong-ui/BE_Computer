package com.example.MyProject.ORDER.service;

import com.example.MyProject.ORDER.dto.payment.PaymentResponse;
import com.example.MyProject.ORDER.dto.payment.SePayPaymentInfoResponse;
import com.example.MyProject.ORDER.dto.payment.SePayWebhookRequest;
import com.example.MyProject.ORDER.dto.payment.VerifyPaymentRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    String createVNPayPayment(Long orderId, HttpServletRequest request);
    PaymentResponse processVNPayReturn(Map<String, String> queryParams);
    PaymentResponse verifyAndProcess(VerifyPaymentRequest request);

    SePayPaymentInfoResponse createSePayPayment(Long orderId);
    PaymentResponse processSePayWebhook(SePayWebhookRequest webhookRequest, String authHeader);

    Page<PaymentResponse> getPaymentsByOrderId(
            Pageable pageable
    );
    PaymentResponse getLatestPaymentByOrderId(Long orderId);
}