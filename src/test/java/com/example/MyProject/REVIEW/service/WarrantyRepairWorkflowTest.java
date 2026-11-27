package com.example.MyProject.REVIEW.service;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.ORDER.entity.OrderItem;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.service.OrdersService;
import com.example.MyProject.PRODUCT.entity.Branch;
import com.example.MyProject.PRODUCT.entity.Categories;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.service.BranchService;
import com.example.MyProject.REVIEW.dto.repair.RepairBookingRequest;
import com.example.MyProject.REVIEW.dto.warranty.WarrantyClaimRequest;
import com.example.MyProject.REVIEW.entity.RepairBooking;
import com.example.MyProject.REVIEW.entity.WarrantyClaim;
import com.example.MyProject.REVIEW.repository.RepairBookingRepository;
import com.example.MyProject.REVIEW.repository.WarrantyClaimRepository;
import com.example.MyProject.REVIEW.service.impl.RepairBookingServiceImpl;
import com.example.MyProject.REVIEW.service.impl.WarrantyServiceImpl;
import com.example.MyProject.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantyRepairWorkflowTest {

    @Mock
    private WarrantyClaimRepository warrantyClaimRepository;

    @Mock
    private OrdersService ordersService;

    @Mock
    private RepairBookingRepository repairBookingRepository;

    @Mock
    private BranchService branchService;

    @InjectMocks
    private WarrantyServiceImpl warrantyService;

    @InjectMocks
    private RepairBookingServiceImpl repairBookingService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void warrantyLookupReturnsPerUnitCardsFromPaymentTimeAndScopesOrderToOwner() {
        Account account = authenticatedAccount(42L, "USER");
        Orders order = paidOrderWithTwoUnits();
        when(ordersService.findOrderForWarrantyLookup(15L, account.getId(), false))
                .thenReturn(Optional.of(order));

        var cards = warrantyService.lookupWarranty("15");

        assertThat(cards).hasSize(2);
        assertThat(cards).extracting("serialCode")
                .containsExactly("BH-15-8-1", "BH-15-8-2");
        assertThat(cards).allSatisfy(card -> assertThat(card.purchaseDate()).isEqualTo(order.getPaidAt()));
        verify(ordersService).findOrderForWarrantyLookup(15L, account.getId(), false);
    }

    @Test
    void warrantyClaimStatusCannotSkipReceivingStage() {
        WarrantyClaim claim = WarrantyClaim.builder().id(9L).status("PENDING").build();
        when(warrantyClaimRepository.findById(9L)).thenReturn(Optional.of(claim));

        assertThatThrownBy(() -> warrantyService.updateClaimStatus(9L, "REPAIRING", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể chuyển trạng thái");
        verify(warrantyClaimRepository, never()).save(any());
    }

    @Test
    void warrantyClaimTargetsTheRequestedUnitAndUsesPaymentDate() {
        Account account = authenticatedAccount(42L, "USER");
        Orders order = paidOrderWithTwoUnits();
        when(ordersService.requireOrderForWarrantyClaim(15L, account.getId())).thenReturn(order);
        when(warrantyClaimRepository.findFirstByOrderIdAndProductIdAndUnitNumberAndStatusIn(
                eq(15L), eq(8L), eq(2), anyList()
        )).thenReturn(Optional.empty());
        when(warrantyClaimRepository.save(any(WarrantyClaim.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        WarrantyClaimRequest request = new WarrantyClaimRequest(
                15L, 8L, 2, "Không lên nguồn", "Thiết bị không hoạt động", "0987654321"
        );

        var claim = warrantyService.createClaim(request);

        assertThat(claim.unitNumber()).isEqualTo(2);
        assertThat(claim.serialCode()).isEqualTo("BH-15-8-2");
        assertThat(claim.warrantyExpireDate()).isEqualTo(order.getPaidAt().plusMonths(12));
        verify(ordersService).requireOrderForWarrantyClaim(15L, account.getId());
    }

    @Test
    void repairBookingRejectsPastAppointment() {
        when(branchService.requireActiveBranchByCodeOrName("HN-THAIHA")).thenReturn(activeBranch());
        RepairBookingRequest request = repairRequest(
                "AT_STORE",
                "HN-THAIHA",
                LocalDate.now().minusDays(1).toString(),
                "09:00 - 10:00"
        );

        assertThatThrownBy(() -> repairBookingService.createBooking(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tương lai");
        verify(repairBookingRepository, never()).save(any());
    }

    @Test
    void repairBookingOnlyAcceptsActiveBranchesAndKnownServiceMethods() {
        when(branchService.requireActiveBranchByCodeOrName("UNKNOWN"))
                .thenThrow(new BusinessException("Chi nhánh không tồn tại hoặc đã ngừng hoạt động"));

        assertThatThrownBy(() -> repairBookingService.createBooking(
                repairRequest("AT_STORE", "UNKNOWN", LocalDate.now().plusDays(1).toString(), "09:00")
        )).isInstanceOf(BusinessException.class).hasMessageContaining("Chi nhánh không tồn tại");

        assertThatThrownBy(() -> repairBookingService.createBooking(
                repairRequest("INVALID", "HN-THAIHA", LocalDate.now().plusDays(1).toString(), "09:00")
        )).isInstanceOf(BusinessException.class).hasMessageContaining("Hình thức sửa chữa không hợp lệ");
        verify(repairBookingRepository, never()).save(any());
    }

    @Test
    void repairBookingCanOnlyAdvanceThroughAllowedStatuses() {
        RepairBooking booking = RepairBooking.builder().id(7L).status("PENDING").build();
        when(repairBookingRepository.findById(7L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> repairBookingService.updateBookingStatus(7L, "IN_PROGRESS", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không thể chuyển trạng thái");
        verify(repairBookingRepository, never()).save(any());
    }

    @Test
    void repairBookingStoresNormalizedDateAndCanonicalBranchName() {
        Branch branch = activeBranch();
        when(branchService.requireActiveBranchByCodeOrName("hn-thaiha")).thenReturn(branch);
        when(repairBookingRepository.save(any(RepairBooking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        RepairBookingRequest request = repairRequest(
                "AT_STORE",
                "hn-thaiha",
                LocalDate.now().plusDays(1).toString(),
                "09:00 – 10:00"
        );

        repairBookingService.createBooking(request);

        ArgumentCaptor<RepairBooking> saved = ArgumentCaptor.forClass(RepairBooking.class);
        verify(repairBookingRepository).save(saved.capture());
        assertThat(saved.getValue().getBranchName()).isEqualTo(branch.getName());
        assertThat(saved.getValue().getAppointmentTime()).isEqualTo("09:00 - 10:00");
        assertThat(saved.getValue().getStatus()).isEqualTo("PENDING");
    }

    private Account authenticatedAccount(Long id, String role) {
        Account account = new Account();
        account.setId(id);
        account.setRole(role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(account, null, account.getAuthorities())
        );
        return account;
    }

    private Orders paidOrderWithTwoUnits() {
        Categories category = new Categories();
        category.setNameCategories("Phụ kiện");
        Products product = new Products();
        product.setId(8L);
        product.setNameProduct("Chuột không dây");
        product.setCategories(category);

        OrderItem item = new OrderItem();
        item.setProducts(product);
        item.setQuantity(2);

        Orders order = new Orders();
        order.setId(15L);
        order.setStatus("COMPLETED");
        order.setCreatedAt(LocalDateTime.now().minusDays(10));
        order.setPaidAt(LocalDateTime.now().minusDays(8));
        order.setOrderItems(List.of(item));
        return order;
    }

    private Branch activeBranch() {
        Branch branch = new Branch();
        branch.setCode("HN-THAIHA");
        branch.setName("HK COMPUTER - Thái Hà");
        branch.setActive(true);
        return branch;
    }

    private RepairBookingRequest repairRequest(
            String method,
            String branchName,
            String date,
            String time
    ) {
        return new RepairBookingRequest(
                "Nguyễn Văn A",
                "0987654321",
                "customer@example.com",
                method,
                branchName,
                null,
                "Laptop Dell",
                "Kiểm tra",
                "Máy không lên nguồn",
                date,
                time
        );
    }
}
