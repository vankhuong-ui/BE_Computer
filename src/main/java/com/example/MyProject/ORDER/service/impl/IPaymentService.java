package com.example.MyProject.ORDER.service.impl;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.ORDER.config.VNPayConfig;
import com.example.MyProject.ORDER.dto.payment.PaymentResponse;
import com.example.MyProject.ORDER.dto.payment.VerifyPaymentRequest;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.entity.Payment;
import com.example.MyProject.ORDER.mapper.PaymentMapper;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import com.example.MyProject.ORDER.repository.PaymentRepository;
import com.example.MyProject.ORDER.service.OrdersService;
import com.example.MyProject.ORDER.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
@Service
@RequiredArgsConstructor
public class IPaymentService implements PaymentService {

    private final OrdersRepository ordersRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final VNPayConfig vnPayConfig;
    private final OrdersService ordersService;

    @Override
    public String createVNPayPayment(Long orderId, HttpServletRequest request) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Orders order = ordersRepository.findByIdAndUseraddressesAccountId(orderId, account.getId())
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại hoặc không thuộc tài khoản của bạn"));
        if (!Objects.equals(account.getId(), order.getUseraddresses().getAccount().getId())) {
            throw new RuntimeException("Đơn hàng này ko thuộc về bạn vì vậy bạn ko thể thanh toán");
        }
        if ("COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("Đơn hàng này đã được thanh toán thành công");
        } else if ("CANCELLED".equals(order.getStatus())) {
            throw new RuntimeException("Đơn hàng này đã bị hủy");
        }

        long amount = order.getTotalAmount().multiply(new BigDecimal(100)).longValue();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getVnpTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", String.valueOf(order.getId()));
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getId());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        // Quan trọng: returnUrl là frontend
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVnpReturnUrl());
        vnp_Params.put("vnp_IpAddr", VNPayConfig.getIpAddress(request));

        ZonedDateTime now = ZonedDateTime.now(
                ZoneId.of("Asia/Ho_Chi_Minh")
        );

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        vnp_Params.put("vnp_CreateDate", now.format(formatter));
        vnp_Params.put("vnp_ExpireDate", now.plusMinutes(15).format(formatter));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String vnp_SecureHash = VNPayConfig.hmacSHA512(vnPayConfig.getSecretKey(), hashData.toString());
        return vnPayConfig.getVnpPayUrl() + "?" + query.toString() + "&vnp_SecureHash=" + vnp_SecureHash;
    }

    @Transactional
    @Override
    public PaymentResponse processVNPayReturn(Map<String, String> queryParams) {
        // Vẫn giữ để dùng nếu cần, nhưng không dùng cho luồng chính
        verifySignature(queryParams);

        String transactionNo = queryParams.get("vnp_TransactionNo");
        String responseCode = queryParams.get("vnp_ResponseCode");
        Long orderId = Long.parseLong(queryParams.get("vnp_TxnRef"));
        BigDecimal amount = new BigDecimal(queryParams.get("vnp_Amount")).divide(new BigDecimal(100));

        boolean isSuccess = "00".equals(responseCode);

        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại: " + orderId));

        if (order.getTotalAmount().compareTo(amount) != 0) {
            throw new RuntimeException("Số tiền thanh toán không khớp với đơn hàng");
        }

        if (isSuccess && !"COMPLETED".equals(order.getStatus())) {
            order.setStatus("COMPLETED");
            ordersRepository.save(order);
        }

        Payment payment = Payment.builder()
                .order(order)
                .transactionId(transactionNo)
                .paymentGateway("VNPAY")
                .amount(amount)
                .status(isSuccess ? "SUCCESS" : "FAILED")
                .responseCode(responseCode)
                .createdAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponse(savedPayment);
    }

    // ✅ Phương thức mới: xác thực từ frontend
    @Transactional
    @Override
    public PaymentResponse verifyAndProcess(VerifyPaymentRequest request) {
        Map<String, String> params = request.getParams();
        // Xác thực chữ ký
        verifySignature(params);

        String responseCode = params.get("vnp_ResponseCode");
        Long orderId = Long.parseLong(params.get("vnp_TxnRef"));
        BigDecimal amount = new BigDecimal(params.get("vnp_Amount")).divide(new BigDecimal(100));

        boolean isSuccess = "00".equals(responseCode);

        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại: " + orderId));

        if (order.getTotalAmount().compareTo(amount) != 0) {
            throw new RuntimeException("Số tiền thanh toán không khớp với đơn hàng");
        }

        // Kiểm tra nếu đơn hàng đã COMPLETED rồi thì không cập nhật nữa
        if (isSuccess && !"COMPLETED".equals(order.getStatus())) {
            order.setStatus("COMPLETED");
            ordersRepository.save(order);
        }

        // Tạo hoặc cập nhật payment record
        Payment payment = Payment.builder()
                .order(order)
                .transactionId(params.get("vnp_TransactionNo"))
                .paymentGateway("VNPAY")
                .amount(amount)
                .status(isSuccess ? "SUCCESS" : "FAILED")
                .responseCode(responseCode)
                .createdAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<PaymentResponse> getPaymentsByOrderId(
            Pageable pageable
    ) {
        return paymentRepository.findAll(pageable)
                .map(paymentMapper::toResponse);
    }



    @Transactional(readOnly = true)
    @Override
    public PaymentResponse getLatestPaymentByOrderId(Long orderId) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!"ADMIN".equalsIgnoreCase(account.getRole()) && !"ROLE_ADMIN".equalsIgnoreCase(account.getRole())) {
            ordersRepository.findByIdAndUseraddressesAccountId(orderId, account.getId())
                    .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại hoặc không thuộc tài khoản của bạn"));
        }
        return paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Chưa có thông tin giao dịch cho đơn hàng này"));
    }

    private void verifySignature(Map<String, String> queryParams) {
        Map<String, String> fields = new HashMap<>(queryParams);
        String vnp_SecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }

        String signValue = VNPayConfig.hmacSHA512(vnPayConfig.getSecretKey(), hashData.toString());
        if (!signValue.equalsIgnoreCase(vnp_SecureHash)) {
            throw new IllegalArgumentException("Invalid Checksum");
        }
    }
}