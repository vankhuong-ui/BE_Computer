package com.example.MyProject.ORDER.service.impl;

import com.example.MyProject.AUTH.repository.AccountRepository;
import com.example.MyProject.ORDER.entity.OrderItem;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.entity.Payment;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import com.example.MyProject.ORDER.repository.PaymentRepository;
import com.example.MyProject.ORDER.service.AdminDashboardService;
import com.example.MyProject.PRODUCT.entity.Branch;
import com.example.MyProject.PRODUCT.entity.Inventory;
import com.example.MyProject.PRODUCT.repository.BranchRepository;
import com.example.MyProject.PRODUCT.repository.InventoryRepository;
import com.example.MyProject.PRODUCT.repository.ProductRepository;
import com.example.MyProject.REVIEW.repository.RepairBookingRepository;
import com.example.MyProject.REVIEW.repository.SupportRepository;
import com.example.MyProject.REVIEW.repository.WarrantyClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IAdminDashboardService implements AdminDashboardService {

    private final OrdersRepository ordersRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final BranchRepository branchRepository;
    private final AccountRepository accountRepository;
    private final WarrantyClaimRepository warrantyClaimRepository;
    private final RepairBookingRepository repairBookingRepository;
    private final SupportRepository supportRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary() {
        List<Orders> allOrders = ordersRepository.findAllWithItemsForDashboard();
        List<Payment> allPayments = paymentRepository.findAll();
        List<Inventory> allInventories = inventoryRepository.findAll();
        List<Branch> activeBranches = branchRepository.findByActiveTrueOrderByIdAsc();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        long completedOrders = 0;
        long unpaidOrders = 0;
        long cancelledOrders = 0;

        Map<LocalDate, BigDecimal> dailyRevenue = new LinkedHashMap<>();
        Map<LocalDate, Integer> dailyOrders = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            dailyRevenue.put(day, BigDecimal.ZERO);
            dailyOrders.put(day, 0);
        }

        Map<String, BigDecimal> revenueByGroup = new LinkedHashMap<>();
        revenueByGroup.put("LAPTOP", BigDecimal.ZERO);
        revenueByGroup.put("PC", BigDecimal.ZERO);
        revenueByGroup.put("ACCESSORY", BigDecimal.ZERO);

        Map<Long, TopProductAcc> productSales = new HashMap<>();

        for (Orders order : allOrders) {
            String status = order.getStatus() != null ? order.getStatus().toUpperCase(Locale.ROOT) : "UNPAID";
            boolean isPaid = "COMPLETED".equals(status) || "PAID".equals(status);
            BigDecimal amount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;

            if (isPaid) {
                completedOrders++;
                totalRevenue = totalRevenue.add(amount);
                if (order.getCreatedAt() != null) {
                    LocalDate orderDay = order.getCreatedAt().toLocalDate();
                    if (dailyRevenue.containsKey(orderDay)) {
                        dailyRevenue.put(orderDay, dailyRevenue.get(orderDay).add(amount));
                        dailyOrders.put(orderDay, dailyOrders.getOrDefault(orderDay, 0) + 1);
                    }
                }
                if (order.getOrderItems() != null) {
                    for (OrderItem item : order.getOrderItems()) {
                        if (item.getProducts() == null) continue;
                        String group = (item.getProducts().getCategories() != null
                                && item.getProducts().getCategories().getGroupType() != null)
                                ? item.getProducts().getCategories().getGroupType().toUpperCase(Locale.ROOT)
                                : "LAPTOP";
                        BigDecimal lineTotal = (item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO)
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                        revenueByGroup.put(group, revenueByGroup.getOrDefault(group, BigDecimal.ZERO).add(lineTotal));

                        Long pid = item.getProducts().getId();
                        TopProductAcc acc = productSales.computeIfAbsent(pid, k -> new TopProductAcc(
                                pid,
                                item.getProducts().getNameProduct(),
                                item.getProducts().getPrice()
                        ));
                        acc.qtySold += item.getQuantity();
                        acc.revenue = acc.revenue.add(lineTotal);
                    }
                }
            } else if ("CANCELLED".equals(status)) {
                cancelledOrders++;
            } else {
                unpaidOrders++;
            }
        }

        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");
        List<Map<String, Object>> revenueLast7Days = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : dailyRevenue.entrySet()) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", e.getKey().toString());
            point.put("label", e.getKey().format(dayFmt));
            point.put("revenue", e.getValue());
            point.put("orders", dailyOrders.getOrDefault(e.getKey(), 0));
            revenueLast7Days.add(point);
        }

        Map<String, Long> gatewayCounts = new LinkedHashMap<>();
        gatewayCounts.put("SEPAY", 0L);
        gatewayCounts.put("VNPAY", 0L);
        for (Payment p : allPayments) {
            if ("SUCCESS".equalsIgnoreCase(p.getStatus()) && p.getPaymentGateway() != null) {
                String gw = p.getPaymentGateway().toUpperCase(Locale.ROOT);
                gatewayCounts.put(gw, gatewayCounts.getOrDefault(gw, 0L) + 1L);
            }
        }

        Map<Long, Integer> stockByProduct = new HashMap<>();
        List<Map<String, Object>> lowStockItems = new ArrayList<>();
        long lowStockCount = 0;

        for (Inventory inv : allInventories) {
            if (inv.getProducts() == null || inv.getBranch() == null || !inv.getBranch().isActive()) {
                continue;
            }
            Long pid = inv.getProducts().getId();
            int qty = Math.max(0, inv.getSoluong());
            stockByProduct.merge(pid, qty, Integer::sum);

            if (inv.getSelled() > 0) {
                TopProductAcc acc = productSales.computeIfAbsent(pid, k -> new TopProductAcc(
                        pid,
                        inv.getProducts().getNameProduct(),
                        inv.getProducts().getPrice()
                ));
                acc.inventorySelled += inv.getSelled();
            }

            if (qty <= 3) {
                lowStockCount++;
                if (lowStockItems.size() < 8) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("inventoryId", inv.getId());
                    row.put("productId", pid);
                    row.put("productName", inv.getProducts().getNameProduct());
                    row.put("branchName", inv.getBranch().getName());
                    row.put("city", inv.getBranch().getCity());
                    row.put("stock", qty);
                    lowStockItems.add(row);
                }
            }
        }

        lowStockItems.sort(Comparator.comparingInt(m -> (Integer) m.get("stock")));

        List<Map<String, Object>> topProducts = productSales.values().stream()
                .sorted(Comparator.comparingInt(TopProductAcc::effectiveSold).reversed()
                        .thenComparing(a -> a.revenue, Comparator.reverseOrder()))
                .limit(5)
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.id);
                    m.put("name", a.name);
                    m.put("price", a.price);
                    m.put("sold", a.effectiveSold());
                    m.put("stock", stockByProduct.getOrDefault(a.id, 0));
                    return m;
                })
                .toList();

        List<Map<String, Object>> recentOrders = allOrders.stream()
                .limit(6)
                .map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", o.getId());
                    m.put("receiverName", o.getReceiverName() != null ? o.getReceiverName()
                            : (o.getUseraddresses() != null ? o.getUseraddresses().getUsername() : "Khách hàng"));
                    m.put("totalAmount", o.getTotalAmount());
                    m.put("status", o.getStatus());
                    m.put("fulfillmentType", o.getFulfillmentType());
                    m.put("createdAt", o.getCreatedAt());
                    return m;
                })
                .toList();

        long pendingWarranties = warrantyClaimRepository.findAll().stream()
                .filter(w -> w.getStatus() == null || (!"COMPLETED".equalsIgnoreCase(w.getStatus()) && !"REJECTED".equalsIgnoreCase(w.getStatus())))
                .count();
        long pendingRepairs = repairBookingRepository.findAll().stream()
                .filter(r -> r.getStatus() == null || "PENDING".equalsIgnoreCase(r.getStatus()) || "CONFIRMED".equalsIgnoreCase(r.getStatus()))
                .count();
        long openSupports = supportRepository.findAll().stream()
                .filter(s -> s.getStatus() == null || !"CLOSED".equalsIgnoreCase(s.getStatus()))
                .count();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("totalRevenue", totalRevenue);
        payload.put("totalOrders", allOrders.size());
        payload.put("completedOrders", completedOrders);
        payload.put("unpaidOrders", unpaidOrders);
        payload.put("cancelledOrders", cancelledOrders);
        payload.put("totalProducts", productRepository.count());
        payload.put("totalCustomers", accountRepository.count());
        payload.put("activeBranches", activeBranches.size());
        payload.put("lowStockCount", lowStockCount);
        payload.put("pendingWarranties", pendingWarranties);
        payload.put("pendingRepairs", pendingRepairs);
        payload.put("openSupports", openSupports);
        payload.put("revenueLast7Days", revenueLast7Days);
        payload.put("revenueByGroup", revenueByGroup);
        payload.put("paymentGateways", gatewayCounts);
        payload.put("topProducts", topProducts);
        payload.put("lowStockItems", lowStockItems);
        payload.put("recentOrders", recentOrders);

        return payload;
    }

    private static class TopProductAcc {
        final Long id;
        final String name;
        final BigDecimal price;
        int qtySold = 0;
        int inventorySelled = 0;
        BigDecimal revenue = BigDecimal.ZERO;

        TopProductAcc(Long id, String name, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.price = price != null ? price : BigDecimal.ZERO;
        }

        int effectiveSold() {
            return Math.max(qtySold, inventorySelled);
        }
    }
}
