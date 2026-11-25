package com.example.MyProject.ORDER.service;

import com.example.MyProject.AUTH.repository.AccountRepository;
import com.example.MyProject.AUTH.service.RedisRateLimiterService;
import com.example.MyProject.ORDER.controller.AdminDashboardController;
import com.example.MyProject.ORDER.entity.OrderItem;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.entity.Payment;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import com.example.MyProject.ORDER.repository.PaymentRepository;
import com.example.MyProject.ORDER.service.impl.IAdminDashboardService;
import com.example.MyProject.PRODUCT.entity.Branch;
import com.example.MyProject.PRODUCT.entity.Categories;
import com.example.MyProject.PRODUCT.entity.Inventory;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.repository.BranchRepository;
import com.example.MyProject.PRODUCT.repository.InventoryRepository;
import com.example.MyProject.PRODUCT.repository.ProductRepository;
import com.example.MyProject.REVIEW.repository.RepairBookingRepository;
import com.example.MyProject.REVIEW.repository.SupportRepository;
import com.example.MyProject.REVIEW.repository.WarrantyClaimRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackendUpgradesTest {

    @Mock private OrdersRepository ordersRepository;
    @Mock private OrdersService ordersService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private WarrantyClaimRepository warrantyClaimRepository;
    @Mock private RepairBookingRepository repairBookingRepository;
    @Mock private SupportRepository supportRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private ListOperations<String, String> listOps;

    @Test
    void orderExpirationSchedulerCancelsAllExpiredUnpaidOrders() {
        when(ordersRepository.findExpiredUnpaidOrderIds(any(LocalDateTime.class)))
                .thenReturn(List.of(101L, 102L));

        OrderExpirationScheduler scheduler = new OrderExpirationScheduler(ordersRepository, ordersService);
        int count = scheduler.cancelExpiredUnpaidOrders();

        assertThat(count).isEqualTo(2);
        verify(ordersService).updateStatus(101L, "CANCELLED");
        verify(ordersService).updateStatus(102L, "CANCELLED");
    }

    @Test
    void redisRateLimiterBlocksWhenThresholdExceededAndStoresChatHistory() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("rl:otp:test@hk.vn")).thenReturn(1L, 2L);

        RedisRateLimiterService limiter = new RedisRateLimiterService(redisTemplate);
        assertThat(limiter.allowRequest("otp:test@hk.vn", 1, Duration.ofSeconds(60))).isTrue();
        assertThat(limiter.allowRequest("otp:test@hk.vn", 1, Duration.ofSeconds(60))).isFalse();

        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.size("chat:hist:s1")).thenReturn(2L);
        limiter.appendChatExchange("s1", "tư vấn laptop dell", "HK có Dell Latitude 7490");
        verify(listOps).rightPushAll(eq("chat:hist:s1"), anyString(), anyString());
    }

    @Test
    void adminDashboardServiceAggregatesRevenueLowStockAndDelegatesCleanlyToController() {
        Categories laptopCat = new Categories();
        laptopCat.setGroupType("LAPTOP");

        Products dell7490 = new Products();
        dell7490.setId(1L);
        dell7490.setNameProduct("Laptop Dell Latitude 7490");
        dell7490.setPrice(new BigDecimal("6990000"));
        dell7490.setCategories(laptopCat);

        OrderItem item = new OrderItem();
        item.setProducts(dell7490);
        item.setQuantity(2);
        item.setPrice(new BigDecimal("6990000"));

        Orders paidOrder = new Orders();
        paidOrder.setId(500L);
        paidOrder.setStatus("COMPLETED");
        paidOrder.setTotalAmount(new BigDecimal("13980000"));
        paidOrder.setReceiverName("Nguyễn Văn Test");
        paidOrder.setCreatedAt(LocalDateTime.now());
        paidOrder.setOrderItems(List.of(item));

        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("HK COMPUTER - Thái Hà");
        branch.setCity("Hà Nội");
        branch.setActive(true);

        Inventory lowInv = new Inventory();
        lowInv.setId(10L);
        lowInv.setProducts(dell7490);
        lowInv.setBranch(branch);
        lowInv.setSoluong(2);
        lowInv.setSelled(5);

        Payment sepayTx = Payment.builder()
                .status("SUCCESS")
                .paymentGateway("SEPAY")
                .amount(new BigDecimal("13980000"))
                .build();

        when(ordersRepository.findAllWithItemsForDashboard()).thenReturn(List.of(paidOrder));
        when(paymentRepository.findAll()).thenReturn(List.of(sepayTx));
        when(inventoryRepository.findAll()).thenReturn(List.of(lowInv));
        when(branchRepository.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(branch));
        when(productRepository.count()).thenReturn(16L);
        when(accountRepository.count()).thenReturn(3L);
        when(warrantyClaimRepository.findAll()).thenReturn(List.of());
        when(repairBookingRepository.findAll()).thenReturn(List.of());
        when(supportRepository.findAll()).thenReturn(List.of());

        IAdminDashboardService service = new IAdminDashboardService(
                ordersRepository, paymentRepository, productRepository, inventoryRepository,
                branchRepository, accountRepository,
                warrantyClaimRepository, repairBookingRepository, supportRepository
        );
        AdminDashboardController controller = new AdminDashboardController(service);

        Map<String, Object> summary = controller.getDashboardSummary().result();
        assertThat(summary.get("totalRevenue")).isEqualTo(new BigDecimal("13980000"));
        assertThat(summary.get("completedOrders")).isEqualTo(1L);
        assertThat(summary.get("lowStockCount")).isEqualTo(1L);
        assertThat(summary.get("totalProducts")).isEqualTo(16L);
    }
}
