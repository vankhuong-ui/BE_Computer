package com.example.MyProject.ORDER.dto.payment;

import lombok.Data;
import java.util.Map;

@Data
public class VerifyPaymentRequest {
    private Map<String, String> params; // toàn bộ query params từ VNPay
}