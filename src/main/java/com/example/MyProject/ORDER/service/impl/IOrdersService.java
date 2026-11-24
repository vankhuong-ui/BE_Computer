package com.example.MyProject.ORDER.service.impl;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.entity.Useraddresses;
import com.example.MyProject.AUTH.repository.UseraddressRepository;
import com.example.MyProject.ORDER.dto.orderItem.OrderItemRequest;
import com.example.MyProject.ORDER.dto.orders.OrdersRequest;
import com.example.MyProject.ORDER.dto.orders.OrdersResponse;
import com.example.MyProject.ORDER.entity.OrderItem;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.entity.Voucher;
import com.example.MyProject.ORDER.mapper.OrdersMapper;
import com.example.MyProject.ORDER.repository.CartRepository;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import com.example.MyProject.ORDER.repository.VoucherRepository;
import com.example.MyProject.ORDER.service.OrderNotificationService;
import com.example.MyProject.ORDER.service.OrdersService;
import com.example.MyProject.ORDER.service.RedisCartService;
import com.example.MyProject.PRODUCT.entity.Branch;
import com.example.MyProject.PRODUCT.entity.Inventory;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.repository.BranchRepository;
import com.example.MyProject.PRODUCT.repository.InventoryRepository;
import com.example.MyProject.PRODUCT.repository.ProductRepository;
import com.example.MyProject.PRODUCT.service.InventoryWebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IOrdersService implements OrdersService {

    private final OrdersMapper ordersMapper;
    private final OrdersRepository ordersRepository;
    private final VoucherRepository voucherRepository;
    private final CartRepository cartRepository;
    private final RedisCartService redisCartService;
    private final OrderNotificationService orderNotificationService;
    private final UseraddressRepository useraddressRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryWebSocketService inventoryWebSocketService;

    @Transactional
    @Override
    public void createOrser(OrdersRequest request) {
        if (request == null) {
            throw new RuntimeException("Thông tin đơn hàng không được null");
        }
        if (request.orderItems() == null || request.orderItems().isEmpty()) {
            throw new RuntimeException("Đơn hàng phải có ít nhất một sản phẩm");
        }

        Account account = (Account) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Long accountId = account.getId();
        boolean isStorePickup = "STORE_PICKUP".equalsIgnoreCase(request.fulfillmentType());

        Branch pickupBranch = null;
        if (isStorePickup) {
            if (request.pickupBranchId() == null) {
                throw new RuntimeException("Vui lòng chọn cơ sở cửa hàng để đến nhận máy");
            }
            pickupBranch = branchRepository.findById(request.pickupBranchId())
                    .filter(Branch::isActive)
                    .orElseThrow(() -> new RuntimeException("Cơ sở nhận hàng không tồn tại hoặc tạm ngưng hoạt động"));
        }

        Useraddresses address;
        if (request.AddressesId() != null) {
            address = useraddressRepository
                    .findByIdAndAccountId(request.AddressesId(), accountId)
                    .orElseThrow(() -> new RuntimeException("Địa chỉ không thuộc tài khoản"));
        } else if (isStorePickup) {
            List<Useraddresses> userAddrs = useraddressRepository.findByAccountId(accountId);
            if (!userAddrs.isEmpty()) {
                address = userAddrs.stream()
                        .filter(a -> Boolean.TRUE.equals(a.getDefaultaddress()))
                        .findFirst()
                        .orElse(userAddrs.get(0));
            } else {
                Useraddresses autoAddr = new Useraddresses();
                autoAddr.setAccount(account);
                autoAddr.setUsername(request.receiverName() != null && !request.receiverName().isBlank()
                        ? request.receiverName().trim()
                        : (account.getAccountname() != null ? account.getAccountname() : "Khách hàng"));
                autoAddr.setPhone(request.receiverPhone() != null && !request.receiverPhone().isBlank()
                        ? request.receiverPhone().trim()
                        : "0900000000");
                autoAddr.setAddress("Nhận tại cửa hàng: " + pickupBranch.getName() + " (" + pickupBranch.getAddress() + ")");
                autoAddr.setDefaultaddress(true);
                address = useraddressRepository.save(autoAddr);
            }
        } else {
            throw new RuntimeException("Vui lòng chọn địa chỉ giao hàng");
        }

        Orders order = new Orders();
        order.setUseraddresses(address);
        order.setFulfillmentType(isStorePickup ? "STORE_PICKUP" : "HOME_DELIVERY");
        order.setPickupBranch(pickupBranch);
        order.setReceiverName(request.receiverName() != null && !request.receiverName().isBlank()
                ? request.receiverName().trim()
                : address.getUsername());
        order.setReceiverPhone(request.receiverPhone() != null && !request.receiverPhone().isBlank()
                ? request.receiverPhone().trim()
                : address.getPhone());
        order.setStatus("UNPAID");
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.orderItems()) {
            if (itemRequest.productId() == null) {
                throw new RuntimeException("Product ID không được null");
            }
            if (itemRequest.quantity() == null || itemRequest.quantity() <= 0) {
                throw new RuntimeException("Số lượng phải lớn hơn 0");
            }

            Products product = productRepository
                    .findById(itemRequest.productId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: #" + itemRequest.productId()));

            Branch fulfillmentBranch;
            if (isStorePickup) {
                fulfillmentBranch = pickupBranch;
                int updatedRows = inventoryRepository.decreaseStockAtBranch(
                        product.getId(),
                        fulfillmentBranch.getId(),
                        itemRequest.quantity()
                );
                if (updatedRows == 0) {
                    throw new RuntimeException(
                            "Sản phẩm \"" + product.getNameProduct() + "\" không đủ số lượng tại cơ sở " + fulfillmentBranch.getName()
                    );
                }
            } else {
                fulfillmentBranch = allocateBranchForHomeDelivery(
                        product,
                        itemRequest.quantity(),
                        request.preferredBranchId(),
                        request.preferredCity(),
                        address.getAddress()
                );
            }

            broadcastProductTotalStock(product.getId());

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            total = total.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProducts(product);
            orderItem.setBranch(fulfillmentBranch);
            orderItem.setQuantity(itemRequest.quantity());
            orderItem.setPrice(product.getPrice());
            orderItems.add(orderItem);
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.voucherCode() != null && !request.voucherCode().isBlank()) {
            Voucher voucher = voucherRepository.findByCodeIgnoreCase(request.voucherCode().trim())
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá \"" + request.voucherCode() + "\" không tồn tại"));
            LocalDateTime now = LocalDateTime.now();
            if (!voucher.isActive()) {
                throw new RuntimeException("Mã giảm giá hiện đã tạm ngưng");
            }
            if (voucher.getUsedCount() >= voucher.getUsageLimit()) {
                throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng");
            }
            if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
                throw new RuntimeException("Mã giảm giá chưa đến thời gian áp dụng");
            }
            if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
                throw new RuntimeException("Mã giảm giá đã hết hạn sử dụng");
            }
            if (total.compareTo(voucher.getMinOrderAmount()) < 0) {
                throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã " + voucher.getCode());
            }
            if (voucher.isFirstOrderOnly() && ordersRepository.countNonCancelledOrdersByAccountId(accountId) > 0) {
                throw new RuntimeException("Mã " + voucher.getCode() + " chỉ áp dụng cho đơn hàng đầu tiên của tài khoản mới");
            }
            if (ordersRepository.countNonCancelledOrdersByAccountIdAndVoucherId(accountId, voucher.getId()) > 0) {
                throw new RuntimeException("Tài khoản của bạn đã sử dụng mã " + voucher.getCode() + " trước đó");
            }

            if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
                discountAmount = total.multiply(voucher.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
                if (voucher.getMaxDiscount() != null && discountAmount.compareTo(voucher.getMaxDiscount()) > 0) {
                    discountAmount = voucher.getMaxDiscount();
                }
            } else {
                discountAmount = voucher.getDiscountValue();
            }
            discountAmount = discountAmount.min(total);

            int updatedVoucher = voucherRepository.incrementUsageIfAvailable(voucher.getId());
            if (updatedVoucher == 0) {
                throw new RuntimeException("Mã giảm giá vừa hết lượt sử dụng");
            }

            order.setVoucher(voucher);
            order.setVoucherCode(voucher.getCode());
            order.setDiscountAmount(discountAmount);
        } else {
            order.setDiscountAmount(BigDecimal.ZERO);
        }

        order.setTotalAmount(total.subtract(discountAmount).max(BigDecimal.ZERO));
        order.setOrderItems(orderItems);
        ordersRepository.save(order);

        // Tự động dọn các sản phẩm vừa đặt khỏi giỏ hàng (DB + Redis)
        List<Long> orderedProductIds = orderItems.stream()
                .map(i -> i.getProducts().getId())
                .distinct()
                .toList();
        if (!orderedProductIds.isEmpty()) {
            cartRepository.deleteByAccountIdAndProductIds(accountId, orderedProductIds);
            for (Long pid : orderedProductIds) {
                try {
                    redisCartService.deleteProduct(accountId, pid);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Branch allocateBranchForHomeDelivery(
            Products product,
            int quantity,
            Long preferredBranchId,
            String preferredCity,
            String shippingAddressText
    ) {
        List<Inventory> available = inventoryRepository.findAllByProductIdWithBranch(product.getId()).stream()
                .filter(i -> i.getBranch() != null && i.getBranch().isActive() && i.getSoluong() >= quantity)
                .toList();

        if (available.isEmpty()) {
            throw new RuntimeException("Sản phẩm \"" + product.getNameProduct() + "\" hiện không đủ số lượng (" + quantity + ") tại các cơ sở.");
        }

        Inventory chosen = null;

        // 1. Ưu tiên cơ sở khách đang chọn trên thanh vị trí
        if (preferredBranchId != null) {
            chosen = available.stream()
                    .filter(i -> preferredBranchId.equals(i.getBranch().getId()))
                    .findFirst()
                    .orElse(null);
        }

        // 2. Ưu tiên cơ sở cùng Tỉnh/Thành phố khách chọn hoặc khớp địa chỉ nhận
        if (chosen == null) {
            String targetCity = (preferredCity != null && !preferredCity.isBlank()) ? preferredCity.trim().toLowerCase() : null;
            String addrLower = shippingAddressText != null ? shippingAddressText.toLowerCase() : "";
            chosen = available.stream()
                    .filter(i -> {
                        String bCity = i.getBranch().getCity() != null ? i.getBranch().getCity().toLowerCase() : "";
                        return (targetCity != null && bCity.equals(targetCity))
                                || (!bCity.isEmpty() && addrLower.contains(bCity));
                    })
                    .max(Comparator.comparingInt(Inventory::getSoluong))
                    .orElse(null);
        }

        // 3. Chọn cơ sở có tồn kho dồi dào nhất trên toàn quốc
        if (chosen == null) {
            chosen = available.stream()
                    .max(Comparator.comparingInt(Inventory::getSoluong))
                    .orElse(available.get(0));
        }

        int updatedRows = inventoryRepository.decreaseStockAtBranch(
                product.getId(),
                chosen.getBranch().getId(),
                quantity
        );
        if (updatedRows == 0) {
            throw new RuntimeException("Sản phẩm \"" + product.getNameProduct() + "\" vừa hết hàng, vui lòng thử lại.");
        }
        return chosen.getBranch();
    }

    @Transactional
    @Override
    public void updateOrderAddresses(Long id, Long addressesid) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long accountid = account.getId();
        Orders orders = ordersRepository.findByIdAndUseraddressesAccountId(id, accountid)
                .orElseThrow(() -> new RuntimeException("order not found"));
        Useraddresses useraddresses = useraddressRepository.findByIdAndAccountId(addressesid, accountid)
                .orElseThrow(() -> new RuntimeException("Địa chỉ bạn đưa không phải của account bạn đã đăng nhập"));
        orders.setUseraddresses(useraddresses);
        ordersRepository.save(orders);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<OrdersResponse> getAllOrders(Pageable pageable) {
        Page<Long> idPage = ordersRepository.findAllOrderIds(pageable);
        if (idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Orders> orders = ordersRepository.findAllOrdersWithDetails(idPage.getContent());
        Map<Long, Orders> orderMap = orders.stream()
                .collect(Collectors.toMap(Orders::getId, order -> order));

        return idPage.map(id -> ordersMapper.toResponse(orderMap.get(id)));
    }

    @Transactional(readOnly = true)
    @Override
    public List<OrdersResponse> showOrders() {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long accountid = account.getId();
        List<Orders> ordersList = ordersRepository.findOrdersWithDetailsByAccountId(accountid);
        return ordersList.stream()
                .map(ordersMapper::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public void updateStatus(Long id, String status) {
        if (!"UNPAID".equals(status)
                && !"COMPLETED".equals(status)
                && !"CANCELLED".equals(status)) {
            throw new RuntimeException("Trạng thái đơn hàng không hợp lệ");
        }

        Orders orders = ordersRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + id));

        String currentStatus = orders.getStatus();
        if (status.equals(currentStatus)) {
            return;
        }

        if ("CANCELLED".equals(currentStatus)) {
            throw new RuntimeException("Đơn hàng đã hủy và hoàn kho, không thể chuyển sang trạng thái khác");
        }

        if ("COMPLETED".equals(currentStatus) && "UNPAID".equals(status)) {
            throw new RuntimeException("Đơn hàng đã hoàn tất thanh toán, không thể chuyển ngược về Chờ thanh toán");
        }

        if ("CANCELLED".equals(status)) {
            restoreStockForOrder(orders);
        }

        orders.setStatus(status);
        if ("COMPLETED".equals(status) && orders.getPaidAt() == null) {
            orders.setPaidAt(LocalDateTime.now());
        }
        ordersRepository.save(orders);

        if ("COMPLETED".equals(status)) {
            orderNotificationService.sendOrderPaidEmailAsync(orders.getId(), "ADMIN_CONFIRM");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrdersResponse searchOrder(Long id) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long accountid = account.getId();

        Orders orders = ordersRepository.findByIdAndAccountIdWithDetails(id, accountid)
                .orElseThrow(() -> new RuntimeException("order not found"));

        return ordersMapper.toResponse(orders);
    }

    @Transactional
    @Override
    public void cancelledOrder(Long id) {
        Account account = (Account) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Long accountId = account.getId();

        Orders orders = ordersRepository
                .findByIdAndAccountIdWithDetails(id, accountId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không thuộc về tài khoản của bạn"));

        if (!"UNPAID".equals(orders.getStatus())) {
            throw new RuntimeException("Đơn hàng này không thể hủy ở trạng thái hiện tại");
        }

        restoreStockForOrder(orders);

        orders.setStatus("CANCELLED");
        ordersRepository.save(orders);
    }

    private void restoreStockForOrder(Orders orders) {
        Long fallbackBranchId = orders.getPickupBranch() != null
                ? orders.getPickupBranch().getId()
                : branchRepository.findAllByOrderByIdAsc().stream().findFirst().map(Branch::getId).orElse(null);

        for (OrderItem item : orders.getOrderItems()) {
            Long branchId = item.getBranch() != null ? item.getBranch().getId() : fallbackBranchId;
            if (branchId != null) {
                int updatedRows = inventoryRepository.increaseStockAtBranch(
                        item.getProducts().getId(),
                        branchId,
                        item.getQuantity()
                );
                if (updatedRows == 0) {
                    List<Inventory> invs = inventoryRepository.findByProducts_Id(item.getProducts().getId());
                    if (!invs.isEmpty()) {
                        Inventory first = invs.get(0);
                        first.setSoluong(first.getSoluong() + item.getQuantity());
                        inventoryRepository.save(first);
                    }
                }
            }
            broadcastProductTotalStock(item.getProducts().getId());
        }

        if (orders.getVoucher() != null && orders.getVoucher().getId() != null) {
            voucherRepository.decrementUsageIfPositive(orders.getVoucher().getId());
        }
    }

    private void broadcastProductTotalStock(Long productId) {
        List<Inventory> all = inventoryRepository.findByProducts_Id(productId);
        int total = all.stream().mapToInt(i -> Math.max(0, i.getSoluong())).sum();
        Long invId = all.isEmpty() ? productId : all.get(0).getId();
        inventoryWebSocketService.sendStockUpdate(invId, productId, total);
    }
}
