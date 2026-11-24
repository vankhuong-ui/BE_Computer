package com.example.MyProject.ORDER.service.impl;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.ORDER.config.SePayConfig;
import com.example.MyProject.ORDER.config.VNPayConfig;
import com.example.MyProject.ORDER.dto.payment.PaymentResponse;
import com.example.MyProject.ORDER.dto.payment.SePayPaymentInfoResponse;
import com.example.MyProject.ORDER.dto.payment.SePayWebhookRequest;
import com.example.MyProject.ORDER.dto.payment.VerifyPaymentRequest;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.entity.Payment;
import com.example.MyProject.ORDER.mapper.PaymentMapper;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import com.example.MyProject.ORDER.repository.PaymentRepository;
import com.example.MyProject.ORDER.service.OrderNotificationService;
import com.example.MyProject.ORDER.service.OrdersService;
import com.example.MyProject.ORDER.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
@Slf4j
@Service
@RequiredArgsConstructor
public class IPaymentService implements PaymentService {

    private final OrdersRepository ordersRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final VNPayConfig vnPayConfig;
    private final SePayConfig sePayConfig;
    private final OrdersService ordersService;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderNotificationService orderNotificationService;

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
            order.setPaidAt(LocalDateTime.now());
            ordersRepository.save(order);
            orderNotificationService.sendOrderPaidEmailAsync(order.getId(), "VNPAY");
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
            order.setPaidAt(LocalDateTime.now());
            ordersRepository.save(order);
            orderNotificationService.sendOrderPaidEmailAsync(order.getId(), "VNPAY");
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
    public SePayPaymentInfoResponse createSePayPayment(Long orderId) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Orders order = ordersRepository.findByIdAndUseraddressesAccountId(orderId, account.getId())
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại hoặc không thuộc tài khoản của bạn"));

        if ("COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("Đơn hàng này đã được thanh toán thành công");
        } else if ("CANCELLED".equals(order.getStatus())) {
            throw new RuntimeException("Đơn hàng này đã bị hủy");
        }

        String prefix = sePayConfig.getPaymentPrefix() != null && !sePayConfig.getPaymentPrefix().isBlank()
                ? sePayConfig.getPaymentPrefix().trim().toUpperCase()
                : "DH";
        String transferContent = prefix + order.getId();
        long amount = order.getTotalAmount().longValue();

        String qrUrl = String.format(
                "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s&template=compact",
                URLEncoder.encode(sePayConfig.getAccountNumber(), StandardCharsets.UTF_8),
                URLEncoder.encode(sePayConfig.getBankName(), StandardCharsets.UTF_8),
                amount,
                URLEncoder.encode(transferContent, StandardCharsets.UTF_8)
        );

        return new SePayPaymentInfoResponse(
                order.getId(),
                order.getTotalAmount(),
                sePayConfig.getBankName(),
                sePayConfig.getAccountNumber(),
                sePayConfig.getAccountName(),
                transferContent,
                qrUrl,
                order.getStatus()
        );
    }

    @Transactional
    @Override
    public PaymentResponse processSePayWebhook(SePayWebhookRequest webhookRequest, String authHeader) {
        // 1. Kiểm tra API Key nếu đã cấu hình trong .env
        String expectedApiKey = sePayConfig.getWebhookApiKey();
        if (expectedApiKey != null && !expectedApiKey.isBlank()) {
            String expectedHeader = "Apikey " + expectedApiKey.trim();
            if (authHeader == null || (!authHeader.trim().equals(expectedHeader) && !authHeader.trim().equals(expectedApiKey.trim()))) {
                throw new SecurityException("Invalid SePay Webhook API Key");
            }
        }

        if (webhookRequest == null) {
            throw new IllegalArgumentException("Webhook payload cannot be null");
        }

        // 2. Chỉ xử lý giao dịch tiền vào ("in") — trả null (HTTP 200) để SePay không retry giao dịch tiền ra
        if (webhookRequest.transferType() != null && !"in".equalsIgnoreCase(webhookRequest.transferType())) {
            log.info("SePay webhook skipped non-incoming transaction: id={}", webhookRequest.id());
            return null;
        }

        // 3. Kiểm tra trùng lặp giao dịch (Idempotency)
        String txId = webhookRequest.id() != null
                ? "SEPAY_" + webhookRequest.id()
                : (webhookRequest.referenceCode() != null ? webhookRequest.referenceCode() : "SEPAY_" + System.currentTimeMillis());

        if (paymentRepository.existsByTransactionId(txId)) {
            Long existingOrderId = extractOrderIdFromContent(webhookRequest.code(), webhookRequest.content());
            if (existingOrderId != null) {
                return paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(existingOrderId)
                        .map(paymentMapper::toResponse)
                        .orElse(null);
            }
            return null;
        }

        // 4. Bóc tách mã đơn hàng (orderId) từ code hoặc content — giao dịch cá nhân không chứa mã đơn trả 200 để tránh retry
        Long orderId = extractOrderIdFromContent(webhookRequest.code(), webhookRequest.content());
        if (orderId == null) {
            log.info("SePay webhook skipped transfer without order prefix: txId={}", txId);
            return null;
        }

        Orders order = ordersRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("SePay webhook order not found: orderId={}, txId={}", orderId, txId);
            return null;
        }

        BigDecimal transferAmount = webhookRequest.transferAmount() != null ? webhookRequest.transferAmount() : BigDecimal.ZERO;
        boolean isOrderCancelled = "CANCELLED".equalsIgnoreCase(order.getStatus());
        boolean isAmountEnough = transferAmount.compareTo(order.getTotalAmount()) >= 0;
        boolean canComplete = !isOrderCancelled && isAmountEnough;

        if (canComplete && !"COMPLETED".equals(order.getStatus())) {
            order.setStatus("COMPLETED");
            order.setPaidAt(LocalDateTime.now());
            ordersRepository.save(order);
            orderNotificationService.sendOrderPaidEmailAsync(order.getId(), "SEPAY");
        }

        String responseCode = isOrderCancelled
                ? "ORDER_CANCELLED"
                : (isAmountEnough ? "00" : "AMOUNT_MISMATCH");

        Payment payment = Payment.builder()
                .order(order)
                .transactionId(txId)
                .paymentGateway("SEPAY")
                .amount(transferAmount)
                .status(canComplete ? "SUCCESS" : "FAILED")
                .responseCode(responseCode)
                .createdAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        PaymentResponse response = paymentMapper.toResponse(savedPayment);

        // 5. Gửi thông báo real-time qua WebSocket cho Frontend
        try {
            messagingTemplate.convertAndSend("/topic/payment/" + order.getId(), response);
        } catch (Exception ignored) {
        }

        return response;
    }

    private Long extractOrderIdFromContent(String code, String content) {
        String prefix = sePayConfig.getPaymentPrefix() != null && !sePayConfig.getPaymentPrefix().isBlank()
                ? sePayConfig.getPaymentPrefix().trim()
                : "DH";
        Pattern pattern = Pattern.compile(Pattern.quote(prefix) + "\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

        if (code != null && !code.isBlank()) {
            Matcher m = pattern.matcher(code);
            if (m.find()) {
                return Long.parseLong(m.group(1));
            }
        }
        if (content != null && !content.isBlank()) {
            Matcher m = pattern.matcher(content);
            if (m.find()) {
                return Long.parseLong(m.group(1));
            }
        }
        return null;
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