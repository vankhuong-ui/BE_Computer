package com.example.MyProject.ORDER.service;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.entity.Useraddresses;
import com.example.MyProject.AUTH.service.UseraddressService;
import com.example.MyProject.ORDER.config.SePayConfig;
import com.example.MyProject.ORDER.config.VNPayConfig;
import com.example.MyProject.ORDER.dto.orderItem.OrderItemRequest;
import com.example.MyProject.ORDER.dto.orders.OrdersRequest;
import com.example.MyProject.ORDER.dto.payment.PaymentResponse;
import com.example.MyProject.ORDER.dto.payment.SePayWebhookRequest;
import com.example.MyProject.ORDER.dto.payment.VerifyPaymentRequest;
import com.example.MyProject.ORDER.entity.OrderItem;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.entity.Payment;
import com.example.MyProject.ORDER.mapper.OrdersMapper;
import com.example.MyProject.ORDER.mapper.PaymentMapper;
import com.example.MyProject.ORDER.repository.CartRepository;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import com.example.MyProject.ORDER.repository.PaymentRepository;
import com.example.MyProject.ORDER.service.impl.OrdersServiceImpl;
import com.example.MyProject.ORDER.service.impl.PaymentServiceImpl;
import com.example.MyProject.PRODUCT.entity.Branch;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.service.BranchService;
import com.example.MyProject.PRODUCT.service.InventoryService;
import com.example.MyProject.PRODUCT.service.ProductService;
import com.example.MyProject.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPaymentFlowTest {

    @Mock private OrdersMapper ordersMapper;
    @Mock private OrdersRepository ordersRepository;
    @Mock private CartRepository cartRepository;
    @Mock private RedisCartService redisCartService;
    @Mock private OrderNotificationService orderNotificationService;
    @Mock private UseraddressService useraddressService;
    @Mock private ProductService productService;
    @Mock private BranchService branchService;
    @Mock private InventoryService inventoryService;

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private OrdersServiceImpl ordersService;
    private PaymentServiceImpl paymentService;
    private VNPayConfig vnPayConfig;
    private SePayConfig sePayConfig;

    @BeforeEach
    void setUp() {
        ordersService = new OrdersServiceImpl(
                ordersMapper, ordersRepository, cartRepository, redisCartService,
                orderNotificationService, useraddressService, productService, branchService, inventoryService
        );

        vnPayConfig = new VNPayConfig();
        ReflectionTestUtils.setField(vnPayConfig, "vnpPayUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(vnPayConfig, "vnpReturnUrl", "http://localhost:5500/payment-result.html");
        ReflectionTestUtils.setField(vnPayConfig, "vnpTmnCode", "TESTCODE");
        ReflectionTestUtils.setField(vnPayConfig, "secretKey", "MY_VNPAY_TEST_SECRET_KEY_123456");

        sePayConfig = new SePayConfig();
        ReflectionTestUtils.setField(sePayConfig, "bankName", "MBBank");
        ReflectionTestUtils.setField(sePayConfig, "accountNumber", "0987654321");
        ReflectionTestUtils.setField(sePayConfig, "accountName", "HK COMPUTER");
        ReflectionTestUtils.setField(sePayConfig, "webhookApiKey", "sepay_secret_999");
        ReflectionTestUtils.setField(sePayConfig, "paymentPrefix", "DH");

        paymentService = new PaymentServiceImpl(
                ordersRepository, paymentRepository, paymentMapper,
                vnPayConfig, sePayConfig, messagingTemplate, orderNotificationService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createHomeDeliveryOrderAllocatesBranchStockCalculatesTotalAndCleansCart() {
        Account customer = authenticateCustomer(7L);

        Useraddresses address = new Useraddresses();
        address.setId(21L);
        address.setUsername("Lê Văn Khách");
        address.setPhone("0912345678");
        address.setAddress("123 Thái Hà, Hà Nội");

        Products dell7490 = new Products();
        dell7490.setId(5L);
        dell7490.setNameProduct("Laptop Dell Latitude 7490");
        dell7490.setPrice(new BigDecimal("6990000"));

        Branch hanoiBranch = new Branch();
        hanoiBranch.setId(1L);
        hanoiBranch.setCode("HN-THAIHA");
        hanoiBranch.setName("HK COMPUTER - Thái Hà");
        hanoiBranch.setCity("Hà Nội");
        hanoiBranch.setActive(true);

        when(useraddressService.resolveAddressForOrder(eq(customer), eq(21L), eq(false), any(), any(), isNull()))
                .thenReturn(address);
        when(productService.requireProductById(5L)).thenReturn(dell7490);
        when(inventoryService.allocateStockForOrder(
                eq(dell7490), eq(2), eq(false), isNull(), eq(1L), eq("Hà Nội"), eq("123 Thái Hà, Hà Nội")
        )).thenReturn(hanoiBranch);

        OrdersRequest req = new OrdersRequest(
                21L,
                "HOME_DELIVERY",
                null,
                1L,
                "Hà Nội",
                "Lê Văn Khách",
                "0912345678",
                List.of(new OrderItemRequest(5L, 2))
        );

        ordersService.createOrser(req);

        ArgumentCaptor<Orders> savedCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(ordersRepository).save(savedCaptor.capture());
        Orders savedOrder = savedCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo("UNPAID");
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("13980000");
        assertThat(savedOrder.getOrderItems()).hasSize(1);
        assertThat(savedOrder.getOrderItems().get(0).getBranch()).isEqualTo(hanoiBranch);

        verify(cartRepository).deleteByAccountIdAndProductIds(7L, List.of(5L));
        verify(redisCartService).deleteProduct(7L, 5L);
    }

    @Test
    void cancellingUnpaidOrderRestoresStockToFulfillmentBranchAndBlocksCancellingPaidOrder() {
        authenticateCustomer(7L);

        Products laptop = new Products();
        laptop.setId(5L);

        Branch hcmBranch = new Branch();
        hcmBranch.setId(2L);

        OrderItem item = new OrderItem();
        item.setProducts(laptop);
        item.setBranch(hcmBranch);
        item.setQuantity(3);

        Orders unpaid = new Orders();
        unpaid.setId(88L);
        unpaid.setStatus("UNPAID");
        unpaid.setOrderItems(List.of(item));

        when(ordersRepository.findByIdAndAccountIdWithDetails(88L, 7L)).thenReturn(Optional.of(unpaid));

        ordersService.cancelledOrder(88L);

        verify(inventoryService).restoreStock(5L, 2L, 3);
        assertThat(unpaid.getStatus()).isEqualTo("CANCELLED");

        Orders completed = new Orders();
        completed.setId(89L);
        completed.setStatus("COMPLETED");
        when(ordersRepository.findByIdAndAccountIdWithDetails(89L, 7L)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> ordersService.cancelledOrder(89L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không thể hủy");
    }

    @Test
    void sePayWebhookValidatesApiKeyCompletesOrderPushesWebSocketAndTriggersEmail() {
        Orders order = new Orders();
        order.setId(55L);
        order.setStatus("UNPAID");
        order.setTotalAmount(new BigDecimal("8490000"));

        SePayWebhookRequest payload = new SePayWebhookRequest(
                9001L,
                "MBBank",
                "2026-11-27 21:00:00",
                "0987654321",
                "DH55",
                "Chuyen khoan don hang DH55",
                "in",
                new BigDecimal("8490000"),
                new BigDecimal("50000000"),
                null,
                "FT26331",
                ""
        );

        assertThatThrownBy(() -> paymentService.processSePayWebhook(payload, "Apikey wrong_key"))
                .isInstanceOf(SecurityException.class);

        when(paymentRepository.existsByTransactionId("SEPAY_9001")).thenReturn(false);
        when(ordersRepository.findById(55L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new PaymentResponse(1L, 55L, p.getTransactionId(), p.getPaymentGateway(), p.getAmount(), p.getStatus(), p.getResponseCode(), p.getCreatedAt());
        });

        PaymentResponse resp = paymentService.processSePayWebhook(payload, "Apikey sepay_secret_999");

        assertThat(resp).isNotNull();
        assertThat(resp.status()).isEqualTo("SUCCESS");
        assertThat(resp.responseCode()).isEqualTo("00");
        assertThat(order.getStatus()).isEqualTo("COMPLETED");
        assertThat(order.getPaidAt()).isNotNull();
        verify(messagingTemplate).convertAndSend(eq("/topic/payment/55"), any(PaymentResponse.class));
        verify(orderNotificationService).sendOrderPaidEmailAsync(55L, "SEPAY");
    }

    @Test
    void vnPayVerificationRejectsTamperedSignatureAndCompletesValidSignedCallback() {
        Orders order = new Orders();
        order.setId(77L);
        order.setStatus("UNPAID");
        order.setTotalAmount(new BigDecimal("6990000"));

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", "699000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "14558899");
        params.put("vnp_TxnRef", "77");

        StringBuilder hashData = new StringBuilder();
        Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            hashData.append(e.getKey()).append('=').append(URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII));
            if (it.hasNext()) hashData.append('&');
        }
        String validHash = VNPayConfig.hmacSHA512("MY_VNPAY_TEST_SECRET_KEY_123456", hashData.toString());

        Map<String, String> tampered = new HashMap<>(params);
        tampered.put("vnp_SecureHash", "deadbeef");
        VerifyPaymentRequest badReq = new VerifyPaymentRequest();
        badReq.setParams(tampered);

        assertThatThrownBy(() -> paymentService.verifyAndProcess(badReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Checksum");

        Map<String, String> signed = new HashMap<>(params);
        signed.put("vnp_SecureHash", validHash);
        VerifyPaymentRequest goodReq = new VerifyPaymentRequest();
        goodReq.setParams(signed);

        when(ordersRepository.findById(77L)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new PaymentResponse(2L, 77L, p.getTransactionId(), p.getPaymentGateway(), p.getAmount(), p.getStatus(), p.getResponseCode(), p.getCreatedAt());
        });

        PaymentResponse verified = paymentService.verifyAndProcess(goodReq);
        assertThat(verified.status()).isEqualTo("SUCCESS");
        assertThat(order.getStatus()).isEqualTo("COMPLETED");
        verify(orderNotificationService).sendOrderPaidEmailAsync(77L, "VNPAY");
    }

    private Account authenticateCustomer(Long id) {
        Account account = new Account();
        account.setId(id);
        account.setAccountname("Khách Test");
        account.setGmail("khach@test.vn");
        account.setRole("USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(account, null, account.getAuthorities())
        );
        return account;
    }
}
