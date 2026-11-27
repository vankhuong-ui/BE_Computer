package com.example.MyProject.ORDER.service;

import com.example.MyProject.ORDER.entity.OrderItem;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import com.example.MyProject.common.BrevoEmailClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Sends asynchronous order receipt and electronic warranty serial notification emails via BrevoEmailClient.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotificationService {

    private final OrdersRepository ordersRepository;
    private final BrevoEmailClient brevoEmailClient;

    @Value("${orders.notification.email-enabled:true}")
    private boolean emailEnabled = true;

    @Value("${app.frontend-url:https://vankhuongdev.id.vn}")
    private String frontendUrl = "https://vankhuongdev.id.vn";

    @Async
    public void sendOrderPaidEmailAsync(Long orderId, String gateway) {
        if (!emailEnabled || orderId == null || !brevoEmailClient.isConfigured()) {
            return;
        }

        try {
            Orders order = ordersRepository.findByIdWithDetails(orderId).orElse(null);
            if (order == null || order.getUseraddresses() == null || order.getUseraddresses().getAccount() == null) {
                return;
            }
            String toEmail = order.getUseraddresses().getAccount().getGmail();
            if (toEmail == null || toEmail.isBlank()) {
                return;
            }

            NumberFormat vnd = NumberFormat.getIntegerInstance(Locale.of("vi", "VN"));
            StringBuilder body = new StringBuilder();
            body.append("Xin chào ").append(order.getReceiverName() != null ? order.getReceiverName() : "Quý khách").append(",\n\n")
                    .append("Cảm ơn bạn đã mua sắm tại HK COMPUTER! Đơn hàng #DH").append(order.getId())
                    .append(" của bạn đã thanh toán thành công qua ").append(gateway).append(".\n\n")
                    .append("CHI TIẾT ĐƠN HÀNG & MÃ BẢO HÀNH ĐIỆN TỬ:\n");

            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    if (item.getProducts() == null) continue;
                    String serial = "BH-" + order.getId() + "-" + item.getProducts().getId() + "-1";
                    body.append("- ").append(item.getProducts().getNameProduct())
                            .append(" x").append(item.getQuantity())
                            .append(" | Đơn giá: ").append(vnd.format(item.getPrice().longValue())).append("đ")
                            .append(" | Serial BH: ").append(serial).append("\n");
                }
            }

            body.append("\nTổng thanh toán: ")
                    .append(vnd.format(order.getTotalAmount().longValue())).append("đ\n")
                    .append("Tra cứu thời hạn bảo hành trực tuyến tại: ")
                    .append(frontendUrl.replaceAll("/$", "")).append("/warranty.html\n\n")
                    .append("Trân trọng,\nĐội ngũ HK COMPUTER");

            brevoEmailClient.sendTextEmail(
                    toEmail,
                    "[HK COMPUTER] Xác nhận thanh toán & Kích hoạt bảo hành đơn #DH" + order.getId(),
                    body.toString()
            );
        } catch (Exception e) {
            log.warn("Could not send order confirmation email for order #{}: {}", orderId, e.getMessage());
        }
    }
}
