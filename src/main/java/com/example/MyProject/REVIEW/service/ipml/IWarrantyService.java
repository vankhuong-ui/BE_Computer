package com.example.MyProject.REVIEW.service.ipml;

import com.example.MyProject.AUTH.common.PhoneValidator;
import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.ORDER.entity.OrderItem;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.REVIEW.dto.warranty.WarrantyCardResponse;
import com.example.MyProject.REVIEW.dto.warranty.WarrantyClaimRequest;
import com.example.MyProject.REVIEW.dto.warranty.WarrantyClaimResponse;
import com.example.MyProject.REVIEW.entity.WarrantyClaim;
import com.example.MyProject.REVIEW.repository.WarrantyClaimRepository;
import com.example.MyProject.REVIEW.service.WarrantyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IWarrantyService implements WarrantyService {

    private final WarrantyClaimRepository warrantyClaimRepository;
    private final OrdersRepository ordersRepository;

    private static final int DEFAULT_WARRANTY_MONTHS = 24;

    @Transactional(readOnly = true)
    @Override
    public List<WarrantyCardResponse> getMyWarrantyCards() {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Orders> orders = ordersRepository.findOrdersWithDetailsByAccountId(account.getId());

        List<WarrantyCardResponse> cards = new ArrayList<>();
        for (Orders order : orders) {
            if (!"COMPLETED".equalsIgnoreCase(order.getStatus()) && !"PAID".equalsIgnoreCase(order.getStatus())) {
                continue;
            }
            cards.addAll(buildCardsFromOrder(order));
        }
        return cards;
    }

    @Transactional(readOnly = true)
    @Override
    public List<WarrantyCardResponse> lookupWarranty(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new RuntimeException("Vui lòng nhập mã bảo hành (Serial) hoặc mã đơn hàng");
        }
        String clean = keyword.trim().toUpperCase();

        // Hỗ trợ tìm theo mã BH dạng BH-{orderId}-{productId} hoặc mã đơn hàng số / DH{orderId}
        Long orderId = null;
        Long filterProductId = null;

        Pattern bhPattern = Pattern.compile("BH-(\\d+)-(\\d+)");
        Matcher bhMatcher = bhPattern.matcher(clean);
        if (bhMatcher.find()) {
            orderId = Long.parseLong(bhMatcher.group(1));
            filterProductId = Long.parseLong(bhMatcher.group(2));
        } else {
            Pattern numPattern = Pattern.compile("(\\d+)");
            Matcher numMatcher = numPattern.matcher(clean);
            if (numMatcher.find()) {
                orderId = Long.parseLong(numMatcher.group(1));
            }
        }

        if (orderId == null) {
            return List.of();
        }

        List<Orders> orders = ordersRepository.findAllOrdersWithDetails(List.of(orderId));
        if (orders.isEmpty()) {
            return List.of();
        }

        Orders order = orders.get(0);
        if (!"COMPLETED".equalsIgnoreCase(order.getStatus()) && !"PAID".equalsIgnoreCase(order.getStatus())) {
            return List.of();
        }

        Long finalFilterProductId = filterProductId;
        return buildCardsFromOrder(order).stream()
                .filter(c -> finalFilterProductId == null || c.productId().equals(finalFilterProductId))
                .toList();
    }

    @Transactional
    @Override
    public WarrantyClaimResponse createClaim(WarrantyClaimRequest request) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (request.orderId() == null || request.productId() == null) {
            throw new RuntimeException("Vui lòng chọn đơn hàng và sản phẩm cần bảo hành");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new RuntimeException("Vui lòng mô tả chi tiết tình trạng lỗi của thiết bị");
        }
        String normalizedPhone = PhoneValidator.validateAndNormalize(request.contactPhone());

        Orders order = ordersRepository.findByIdAndAccountIdWithDetails(request.orderId(), account.getId())
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại hoặc không thuộc tài khoản của bạn"));

        if (!"COMPLETED".equalsIgnoreCase(order.getStatus()) && !"PAID".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Chỉ những đơn hàng đã hoàn tất thanh toán mới được kích hoạt bảo hành");
        }

        OrderItem matchedItem = order.getOrderItems().stream()
                .filter(item -> item.getProducts() != null && item.getProducts().getId().equals(request.productId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại trong đơn hàng này"));

        Products product = matchedItem.getProducts();
        LocalDateTime purchaseDate = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
        int warrantyMonths = resolveWarrantyMonths(product);
        LocalDateTime expireDate = purchaseDate.plusMonths(warrantyMonths);

        if (expireDate.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Sản phẩm này đã hết thời hạn bảo hành vào ngày " + expireDate.toLocalDate());
        }

        // Kiểm tra xem có yêu cầu bảo hành nào đang xử lý không
        warrantyClaimRepository.findFirstByOrderIdAndProductIdAndStatusIn(
                order.getId(),
                product.getId(),
                List.of("PENDING", "RECEIVED", "REPAIRING")
        ).ifPresent(existing -> {
            throw new RuntimeException("Sản phẩm này đang có một yêu cầu bảo hành đang được xử lý (Mã #" + existing.getId() + ")");
        });

        String serialCode = buildSerialCode(order.getId(), product.getId());

        WarrantyClaim claim = WarrantyClaim.builder()
                .order(order)
                .product(product)
                .account(account)
                .serialCode(serialCode)
                .issueType(request.issueType() != null && !request.issueType().isBlank() ? request.issueType() : "Lỗi phần cứng / Kỹ thuật")
                .description(request.description().trim())
                .contactPhone(normalizedPhone)
                .status("PENDING")
                .warrantyMonths(warrantyMonths)
                .warrantyExpireDate(expireDate)
                .build();

        WarrantyClaim saved = warrantyClaimRepository.save(claim);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<WarrantyClaimResponse> getMyClaims(Pageable pageable) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return warrantyClaimRepository.findByAccountId(account.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<WarrantyClaimResponse> getAllClaims(Pageable pageable) {
        return warrantyClaimRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional
    @Override
    public WarrantyClaimResponse updateClaimStatus(Long id, String status, String adminNote) {
        List<String> validStatuses = List.of("PENDING", "RECEIVED", "REPAIRING", "COMPLETED", "REJECTED");
        String upperStatus = status != null ? status.trim().toUpperCase() : "";
        if (!validStatuses.contains(upperStatus)) {
            throw new RuntimeException("Trạng thái bảo hành không hợp lệ: " + status);
        }

        WarrantyClaim claim = warrantyClaimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu yêu cầu bảo hành #" + id));

        claim.setStatus(upperStatus);
        if (adminNote != null) {
            claim.setAdminNote(adminNote.trim());
        }

        WarrantyClaim saved = warrantyClaimRepository.save(claim);
        return toResponse(saved);
    }

    private List<WarrantyCardResponse> buildCardsFromOrder(Orders order) {
        List<WarrantyCardResponse> result = new ArrayList<>();
        if (order.getOrderItems() == null) {
            return result;
        }
        LocalDateTime purchaseDate = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
        LocalDateTime now = LocalDateTime.now();

        for (OrderItem item : order.getOrderItems()) {
            Products p = item.getProducts();
            if (p == null) continue;

            int months = resolveWarrantyMonths(p);
            LocalDateTime expireDate = purchaseDate.plusMonths(months);
            long remainingDays = Math.max(0, Duration.between(now, expireDate).toDays());
            String status = expireDate.isAfter(now) ? "ACTIVE" : "EXPIRED";

            result.add(new WarrantyCardResponse(
                    order.getId(),
                    p.getId(),
                    p.getNameProduct(),
                    p.getImage(),
                    item.getQuantity(),
                    buildSerialCode(order.getId(), p.getId()),
                    purchaseDate,
                    expireDate,
                    months,
                    remainingDays,
                    status
            ));
        }
        return result;
    }

    private int resolveWarrantyMonths(Products product) {
        if (product.getCategories() != null && product.getCategories().getNameCategories() != null) {
            String cat = product.getCategories().getNameCategories().toLowerCase();
            if (cat.contains("cpu") || cat.contains("main") || cat.contains("vga") || cat.contains("ram")) {
                return 36;
            }
            if (cat.contains("phụ kiện") || cat.contains("chuột") || cat.contains("phím")) {
                return 12;
            }
        }
        return DEFAULT_WARRANTY_MONTHS;
    }

    private String buildSerialCode(Long orderId, Long productId) {
        return String.format("BH-%d-%d", orderId, productId);
    }

    private WarrantyClaimResponse toResponse(WarrantyClaim claim) {
        return new WarrantyClaimResponse(
                claim.getId(),
                claim.getOrder() != null ? claim.getOrder().getId() : null,
                claim.getProduct() != null ? claim.getProduct().getId() : null,
                claim.getProduct() != null ? claim.getProduct().getNameProduct() : null,
                claim.getProduct() != null ? claim.getProduct().getImage() : null,
                claim.getAccount() != null ? claim.getAccount().getId() : null,
                claim.getAccount() != null ? claim.getAccount().getGmail() : null,
                claim.getSerialCode(),
                claim.getIssueType(),
                claim.getDescription(),
                claim.getContactPhone(),
                claim.getStatus(),
                claim.getAdminNote(),
                claim.getWarrantyMonths(),
                claim.getWarrantyExpireDate(),
                claim.getCreatedAt(),
                claim.getUpdatedAt()
        );
    }
}
