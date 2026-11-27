package com.example.MyProject.ORDER.service.impl;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.entity.Useraddresses;
import com.example.MyProject.AUTH.service.UseraddressService;
import com.example.MyProject.ORDER.dto.orderItem.OrderItemRequest;
import com.example.MyProject.ORDER.dto.orders.OrdersRequest;
import com.example.MyProject.ORDER.dto.orders.OrdersResponse;
import com.example.MyProject.ORDER.entity.OrderItem;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.mapper.OrdersMapper;
import com.example.MyProject.ORDER.repository.CartRepository;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import com.example.MyProject.ORDER.service.OrderNotificationService;
import com.example.MyProject.ORDER.service.OrdersService;
import com.example.MyProject.ORDER.service.RedisCartService;
import com.example.MyProject.PRODUCT.entity.Branch;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.service.BranchService;
import com.example.MyProject.PRODUCT.service.InventoryService;
import com.example.MyProject.PRODUCT.service.ProductService;
import com.example.MyProject.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdersServiceImpl implements OrdersService {

    private final OrdersMapper ordersMapper;
    private final OrdersRepository ordersRepository;
    private final CartRepository cartRepository;
    private final RedisCartService redisCartService;
    private final OrderNotificationService orderNotificationService;
    private final UseraddressService useraddressService;
    private final ProductService productService;
    private final BranchService branchService;
    private final InventoryService inventoryService;

    @Transactional
    @Override
    public void createOrser(OrdersRequest request) {
        if (request == null) {
            throw new BusinessException("Thông tin đơn hàng không được null");
        }
        if (request.orderItems() == null || request.orderItems().isEmpty()) {
            throw new BusinessException("Đơn hàng phải có ít nhất một sản phẩm");
        }

        Account account = getCurrentAccount();
        Long accountId = account.getId();
        boolean isStorePickup = "STORE_PICKUP".equalsIgnoreCase(request.fulfillmentType());

        Branch pickupBranch = null;
        String pickupSummary = null;
        if (isStorePickup) {
            if (request.pickupBranchId() == null) {
                throw new BusinessException("Vui lòng chọn cơ sở cửa hàng để đến nhận máy");
            }
            pickupBranch = branchService.requireActiveBranch(request.pickupBranchId());
            pickupSummary = "Nhận tại cửa hàng: " + pickupBranch.getName() + " (" + pickupBranch.getAddress() + ")";
        }

        Useraddresses address = useraddressService.resolveAddressForOrder(
                account,
                request.AddressesId(),
                isStorePickup,
                request.receiverName(),
                request.receiverPhone(),
                pickupSummary
        );

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
                throw new BusinessException("Product ID không được null");
            }
            if (itemRequest.quantity() == null || itemRequest.quantity() <= 0) {
                throw new BusinessException("Số lượng phải lớn hơn 0");
            }

            Products product = productService.requireProductById(itemRequest.productId());

            Branch fulfillmentBranch = inventoryService.allocateStockForOrder(
                    product,
                    itemRequest.quantity(),
                    isStorePickup,
                    pickupBranch,
                    request.preferredBranchId(),
                    request.preferredCity(),
                    address.getAddress()
            );

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

        order.setTotalAmount(total.max(BigDecimal.ZERO));
        order.setOrderItems(orderItems);
        ordersRepository.save(order);

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

    @Transactional
    @Override
    public void updateOrderAddresses(Long id, Long addressesid) {
        Account account = getCurrentAccount();
        Long accountId = account.getId();
        Orders orders = ordersRepository.findByIdAndUseraddressesAccountId(id, accountId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng"));
        Useraddresses useraddresses = useraddressService.requireOwnedAddress(addressesid, accountId);
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
        Account account = getCurrentAccount();
        return ordersRepository.findOrdersWithDetailsByAccountId(account.getId()).stream()
                .map(ordersMapper::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public void updateStatus(Long id, String status) {
        if (!"UNPAID".equals(status)
                && !"COMPLETED".equals(status)
                && !"CANCELLED".equals(status)) {
            throw new BusinessException("Trạng thái đơn hàng không hợp lệ");
        }

        Orders orders = ordersRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn hàng #" + id));

        String currentStatus = orders.getStatus();
        if (status.equals(currentStatus)) {
            return;
        }

        if ("CANCELLED".equals(currentStatus)) {
            throw new BusinessException("Đơn hàng đã hủy và hoàn kho, không thể chuyển sang trạng thái khác");
        }

        if ("COMPLETED".equals(currentStatus) && "UNPAID".equals(status)) {
            throw new BusinessException("Đơn hàng đã hoàn tất thanh toán, không thể chuyển ngược về Chờ thanh toán");
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
        Account account = getCurrentAccount();
        Orders orders = requireOwnedOrderWithDetails(id, account.getId());
        return ordersMapper.toResponse(orders);
    }

    @Transactional
    @Override
    public void cancelledOrder(Long id) {
        Account account = getCurrentAccount();
        Orders orders = requireOwnedOrderWithDetails(id, account.getId());

        if (!"UNPAID".equals(orders.getStatus())) {
            throw new BusinessException("Đơn hàng này không thể hủy ở trạng thái hiện tại");
        }

        restoreStockForOrder(orders);
        orders.setStatus("CANCELLED");
        ordersRepository.save(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Orders> findOrdersWithDetailsByAccount(Long accountId) {
        return ordersRepository.findOrdersWithDetailsByAccountId(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Orders> findOrderForWarrantyLookup(Long orderId, Long accountId, boolean isAdmin) {
        return isAdmin
                ? ordersRepository.findByIdWithDetails(orderId)
                : ordersRepository.findByIdAndAccountIdWithDetails(orderId, accountId);
    }

    @Override
    @Transactional
    public Orders requireOrderForWarrantyClaim(Long orderId, Long accountId) {
        return ordersRepository.findForWarrantyClaimByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new BusinessException("Đơn hàng không tồn tại hoặc không thuộc tài khoản của bạn"));
    }

    @Override
    @Transactional(readOnly = true)
    public Orders requireOwnedOrderWithDetails(Long orderId, Long accountId) {
        return ordersRepository.findByIdAndAccountIdWithDetails(orderId, accountId)
                .orElseThrow(() -> new BusinessException("Đơn hàng không tồn tại hoặc không thuộc tài khoản của bạn"));
    }

    private void restoreStockForOrder(Orders orders) {
        Long fallbackBranchId = orders.getPickupBranch() != null
                ? orders.getPickupBranch().getId()
                : branchService.findFirstBranchId();

        for (OrderItem item : orders.getOrderItems()) {
            Long branchId = item.getBranch() != null ? item.getBranch().getId() : fallbackBranchId;
            inventoryService.restoreStock(item.getProducts().getId(), branchId, item.getQuantity());
        }
    }

    private Account getCurrentAccount() {
        return (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
