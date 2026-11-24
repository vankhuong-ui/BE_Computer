package com.example.MyProject.ORDER.controller;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.ORDER.common.ApiResponse;
import com.example.MyProject.ORDER.common.BaseController;
import com.example.MyProject.ORDER.dto.voucher.VoucherRequest;
import com.example.MyProject.ORDER.dto.voucher.VoucherResponse;
import com.example.MyProject.ORDER.entity.Voucher;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import com.example.MyProject.ORDER.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vouchers")
public class VoucherController extends BaseController {

    private final VoucherRepository voucherRepository;
    private final OrdersRepository ordersRepository;

    @GetMapping("/available")
    @Transactional(readOnly = true)
    public ApiResponse<List<VoucherResponse>> listAvailableVouchers() {
        LocalDateTime now = LocalDateTime.now();
        List<VoucherResponse> list = voucherRepository.findByActiveTrueOrderByFirstOrderOnlyDescIdAsc().stream()
                .filter(v -> v.getUsedCount() < v.getUsageLimit())
                .filter(v -> v.getStartDate() == null || !now.isBefore(v.getStartDate()))
                .filter(v -> v.getEndDate() == null || !now.isAfter(v.getEndDate()))
                .map(this::toResponse)
                .toList();
        return successDataResponse("Get available vouchers success", list);
    }

    @GetMapping("/preview")
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> previewVoucher(
            @RequestParam("code") String code,
            @RequestParam("orderAmount") BigDecimal orderAmount
    ) {
        Voucher v = voucherRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new RuntimeException("Mã giảm giá \"" + code + "\" không tồn tại"));

        Long accountId = getCurrentAccountIdOrNull();
        BigDecimal discount = validateAndCalculateDiscount(v, accountId, orderAmount);
        BigDecimal finalAmount = orderAmount.subtract(discount).max(BigDecimal.ZERO);

        return successDataResponse("Áp dụng mã giảm giá hợp lệ", Map.of(
                "voucher", toResponse(v),
                "discountAmount", discount,
                "finalAmount", finalAmount
        ));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Transactional(readOnly = true)
    public ApiResponse<List<VoucherResponse>> listAdminVouchers() {
        return successDataResponse("Get all vouchers success",
                voucherRepository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList());
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Transactional
    public ApiResponse<VoucherResponse> createVoucher(@RequestBody VoucherRequest req) {
        if (req.code() == null || req.code().isBlank()) {
            throw new RuntimeException("Mã voucher không được để trống");
        }
        String code = req.code().trim().toUpperCase();
        if (voucherRepository.existsByCodeIgnoreCase(code)) {
            throw new RuntimeException("Mã voucher " + code + " đã tồn tại");
        }
        Voucher v = new Voucher();
        applyRequest(v, req, code);
        v.setUsedCount(0);
        return successDataResponse("Tạo mã giảm giá thành công", toResponse(voucherRepository.save(v)));
    }

    @PutMapping("/{id}/update")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Transactional
    public ApiResponse<VoucherResponse> updateVoucher(@PathVariable("id") Long id, @RequestBody VoucherRequest req) {
        Voucher v = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá #" + id));
        String code = (req.code() != null && !req.code().isBlank()) ? req.code().trim().toUpperCase() : v.getCode();
        voucherRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("Mã voucher " + code + " đã được sử dụng");
            }
        });
        applyRequest(v, req, code);
        return successDataResponse("Cập nhật mã giảm giá thành công", toResponse(voucherRepository.save(v)));
    }

    @DeleteMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Transactional
    public ApiResponse<String> deleteVoucher(@PathVariable("id") Long id) {
        Voucher v = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá #" + id));
        if (v.getUsedCount() > 0) {
            v.setActive(false);
            voucherRepository.save(v);
            return successResponse("Mã đã có lịch sử sử dụng nên đã được chuyển sang Tạm ngưng.");
        }
        voucherRepository.delete(v);
        return successResponse("Đã xóa mã giảm giá");
    }

    private void applyRequest(Voucher v, VoucherRequest req, String code) {
        v.setCode(code);
        v.setTitle(req.title() != null && !req.title().isBlank() ? req.title().trim() : code);
        v.setDescription(req.description() != null ? req.description().trim() : "");
        v.setDiscountType("PERCENT".equalsIgnoreCase(req.discountType()) ? "PERCENT" : "FIXED");
        v.setDiscountValue(req.discountValue() != null && req.discountValue().compareTo(BigDecimal.ZERO) > 0
                ? req.discountValue() : BigDecimal.ZERO);
        v.setMaxDiscount(req.maxDiscount());
        v.setMinOrderAmount(req.minOrderAmount() != null && req.minOrderAmount().compareTo(BigDecimal.ZERO) >= 0
                ? req.minOrderAmount() : BigDecimal.ZERO);
        v.setFirstOrderOnly(Boolean.TRUE.equals(req.firstOrderOnly()));
        v.setUsageLimit(req.usageLimit() != null && req.usageLimit() > 0 ? req.usageLimit() : 100);
        v.setStartDate(req.startDate());
        v.setEndDate(req.endDate());
        v.setActive(req.active() == null || req.active());
    }

    private BigDecimal validateAndCalculateDiscount(Voucher v, Long accountId, BigDecimal orderAmount) {
        LocalDateTime now = LocalDateTime.now();
        if (!v.isActive()) {
            throw new RuntimeException("Mã giảm giá hiện đã tạm ngưng");
        }
        if (v.getUsedCount() >= v.getUsageLimit()) {
            throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng");
        }
        if (v.getStartDate() != null && now.isBefore(v.getStartDate())) {
            throw new RuntimeException("Mã giảm giá chưa đến thời gian áp dụng");
        }
        if (v.getEndDate() != null && now.isAfter(v.getEndDate())) {
            throw new RuntimeException("Mã giảm giá đã hết hạn sử dụng");
        }
        if (orderAmount == null || orderAmount.compareTo(v.getMinOrderAmount()) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu " + v.getMinOrderAmount().toPlainString() + "đ để dùng mã này");
        }
        if (accountId != null) {
            if (v.isFirstOrderOnly() && ordersRepository.countNonCancelledOrdersByAccountId(accountId) > 0) {
                throw new RuntimeException("Mã " + v.getCode() + " chỉ áp dụng cho đơn hàng đầu tiên của tài khoản mới");
            }
            if (ordersRepository.countNonCancelledOrdersByAccountIdAndVoucherId(accountId, v.getId()) > 0) {
                throw new RuntimeException("Tài khoản của bạn đã sử dụng mã " + v.getCode() + " trước đó");
            }
        }

        BigDecimal discount;
        if ("PERCENT".equalsIgnoreCase(v.getDiscountType())) {
            discount = orderAmount.multiply(v.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (v.getMaxDiscount() != null && discount.compareTo(v.getMaxDiscount()) > 0) {
                discount = v.getMaxDiscount();
            }
        } else {
            discount = v.getDiscountValue();
        }
        return discount.min(orderAmount);
    }

    private Long getCurrentAccountIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Account acc) {
            return acc.getId();
        }
        return null;
    }

    private VoucherResponse toResponse(Voucher v) {
        return new VoucherResponse(
                v.getId(),
                v.getCode(),
                v.getTitle(),
                v.getDescription(),
                v.getDiscountType(),
                v.getDiscountValue(),
                v.getMaxDiscount(),
                v.getMinOrderAmount(),
                v.isFirstOrderOnly(),
                v.getUsageLimit(),
                v.getUsedCount(),
                v.getStartDate(),
                v.getEndDate(),
                v.isActive()
        );
    }
}
