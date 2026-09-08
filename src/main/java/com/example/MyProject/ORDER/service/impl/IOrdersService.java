package com.example.MyProject.ORDER.service.impl;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.entity.Useraddresses;
import com.example.MyProject.AUTH.repository.UseraddressRepository;
import com.example.MyProject.ORDER.dto.orderItem.OrderItemRequest;
import com.example.MyProject.ORDER.dto.orderItem.OrderItemResponse;
import com.example.MyProject.ORDER.dto.orders.OrdersRequest;
import com.example.MyProject.ORDER.dto.orders.OrdersResponse;
import com.example.MyProject.ORDER.entity.OrderItem;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.mapper.OrdersMapper;
import com.example.MyProject.ORDER.repository.OrderItemRepository;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import com.example.MyProject.ORDER.service.OrdersService;
import com.example.MyProject.PRODUCT.dto.product.response.ResponseProduct;
import com.example.MyProject.PRODUCT.entity.Inventory;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.mapper.ProductMapper;
import com.example.MyProject.PRODUCT.repository.InventoryRepository;
import com.example.MyProject.PRODUCT.repository.ProductRepository;
import com.example.MyProject.PRODUCT.service.InventoryWebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IOrdersService implements OrdersService {

    private final OrdersMapper ordersMapper;

    private final OrdersRepository ordersRepository;

    private final UseraddressRepository useraddressRepository;

    private final ProductRepository productRepository;

    private final InventoryRepository inventoryRepository;
    private final InventoryWebSocketService inventoryWebSocketService;

    @Transactional
    @Override
    public void createOrser(OrdersRequest request) {

        if (request == null) {
            throw new RuntimeException("Thông tin đơn hàng không được null");
        }

        Account account = (Account) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Long accountId = account.getId();

        Useraddresses address = useraddressRepository
                .findByIdAndAccountId(request.AddressesId(), accountId)
                .orElseThrow(() ->
                        new RuntimeException("Địa chỉ không thuộc tài khoản")
                );

        if (request.orderItems() == null || request.orderItems().isEmpty()) {
            throw new RuntimeException("Đơn hàng phải có ít nhất một sản phẩm");
        }

        Orders order = new Orders();
        order.setUseraddresses(address);
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
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Product không tồn tại: "
                                            + itemRequest.productId()
                            )
                    );

            // Trừ kho
            int updatedRows = inventoryRepository.decreaseStock(
                    product.getId(),
                    itemRequest.quantity()
            );

            if (updatedRows == 0) {
                throw new RuntimeException(
                        "Sản phẩm " + product.getNameProduct()
                                + " không đủ số lượng"
                );
            }

            // Lấy Inventory
            Inventory inventory = inventoryRepository
                    .findByProducts_Id(product.getId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Inventory không tồn tại"
                            )
                    );

            // Gửi WebSocket
            inventoryWebSocketService.sendStockUpdate(
                    inventory.getId(),
                    product.getId(),
                    inventory.getSoluong()
            );

            // Tính tiền
            BigDecimal itemTotal = product.getPrice()
                    .multiply(
                            BigDecimal.valueOf(
                                    itemRequest.quantity()
                            )
                    );

            total = total.add(itemTotal);

            // Tạo OrderItem
            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order);
            orderItem.setProducts(product);
            orderItem.setQuantity(itemRequest.quantity());

            // Lưu giá tại thời điểm mua
            orderItem.setPrice(product.getPrice());

            orderItems.add(orderItem);
        }

        order.setTotalAmount(total);
        order.setOrderItems(orderItems);

        ordersRepository.save(order);
    }

    @Transactional
    @Override
    public void updateOrderAddresses(Long id, Long addressesid) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long accountid = account.getId();
        Orders orders = ordersRepository.findByIdAndUseraddressesAccountId(id, accountid).orElseThrow(() -> new RuntimeException("order not found"));
        Useraddresses useraddresses = useraddressRepository.findByIdAndAccountId(addressesid, accountid).orElseThrow(() -> new RuntimeException("Địa chỉ bạn đưa không phải của account bạn đã  đăng nhập"));
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

        List<Orders> orders =
                ordersRepository.findAllOrdersWithDetails(idPage.getContent());

        Map<Long, Orders> orderMap = orders.stream()
                .collect(Collectors.toMap(
                        Orders::getId,
                        order -> order
                ));

        return idPage.map(id ->
                ordersMapper.toResponse(orderMap.get(id))
        );
    }

    @Transactional(readOnly = true)
    @Override
    public List<OrdersResponse> showOrders() {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long accountid = account.getId();
        List<Orders> ordersList = ordersRepository.findOrdersWithDetailsByAccountId(accountid);

//        List<OrderItemResponse> orderItemResponses = new ArrayList<>();
//        for (Orders orders : ordersList) {
//            List<OrdersResponse> ordersResponses = new ArrayList<>();
//            List<OrderItem> listordersitem = orderItemRepository.findByOrderId(orders.getId());
//            for (OrderItem orderItem1 : listordersitem) {
//                Products products = orderItem1.getProducts();
//                ResponseProduct responseProduct = productMapper.toReponse(products);
//                OrderItemResponse orderItemResponse = new OrderItemResponse(orderItem1.getId(), orderItem1.getQuantity(), orderItem1.getPrice(), responseProduct);
//                orderItemResponses.add(orderItemResponse);
//            }
//            OrdersResponse ordersResponse = ordersMapper.toResponse(orders);
//
//        }
//       List<OrdersResponse> ordersResponses=new ArrayList<>();
//       for(Orders orders: ordersList){
//
//       }
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

            throw new RuntimeException("Status không hợp lệ");
        }
        Orders orders = ordersRepository.findById(id).orElseThrow(() -> new RuntimeException("order not found"));
        orders.setStatus(status);
        ordersRepository.save(orders);
    }

    @Override
    public OrdersResponse searchOrder(Long id) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long accountid = account.getId();

        // Sửa thành hàm có JOIN FETCH để lấy đủ item và product, tránh lỗi Lazy
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
                .orElseThrow(() ->
                        new RuntimeException(
                                "Đơn hàng không thuộc về account của bạn"
                        )
                );


        // Chỉ cho phép hủy khi chưa thanh toán
        if (!"UNPAID".equals(orders.getStatus())) {
            throw new RuntimeException(
                    "Đơn hàng này không thể hủy ở trạng thái hiện tại"
            );
        }


        for (OrderItem item : orders.getOrderItems()) {

            int updatedRows = inventoryRepository.increaseStock(
                    item.getProducts().getId(),
                    item.getQuantity()
            );

            if (updatedRows == 0) {
                throw new RuntimeException(
                        "Không thể hoàn kho cho sản phẩm: "
                                + item.getProducts().getNameProduct()
                );
            }

            Inventory inventory = inventoryRepository
                    .findByProducts_Id(item.getProducts().getId())
                    .orElseThrow(() ->
                            new RuntimeException("Inventory không tồn tại")
                    );

            inventoryWebSocketService.sendStockUpdate(
                    inventory.getId(),
                    item.getProducts().getId(),
                    inventory.getSoluong()
            );
        }

        // Sau khi hoàn kho thành công mới chuyển status
        orders.setStatus("CANCELLED");
        ordersRepository.save(orders);
    }
}
