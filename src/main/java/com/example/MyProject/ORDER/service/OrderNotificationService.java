package com.example.MyProject.ORDER.service;

import com.example.MyProject.ORDER.entity.OrderItem;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Comparator;

/**
 * Sends asynchronous order receipt and electronic warranty serial notification emails via Brevo API v3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotificationService {

    private final OrdersRepository ordersRepository;

    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey = "";

    @Value("${BREVO_FROM_EMAIL:}")
    private String brevoFromEmail = "";

    @Value("${app.frontend-url:https://vankhuongdev.id.vn}")
    private String frontendUrl = "https://vankhuongdev.id.vn";

    @Async
    public void sendOrderPaidEmailAsync(Long orderId, String gateway) {
        if (orderId == null || !hasValidBrevoConfig()) {
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
                Map<Long, Integer> nextUnitByProduct = new HashMap<>();
                for (OrderItem item : order.getOrderItems().stream()
                        .sorted(Comparator.comparing(item -> item.getId() == null ? Long.MAX_VALUE : item.getId()))
                        .toList()) {
                    if (item.getProducts() == null) continue;
                    Long productId = item.getProducts().getId();
                    int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                    int firstUnitNumber = nextUnitByProduct.getOrDefault(productId, 0) + 1;
                    body.append("- ").append(item.getProducts().getNameProduct())
                            .append(" x").append(quantity)
                            .append(" | Đơn giá: ").append(vnd.format(item.getPrice().longValue())).append("đ\n");
                    for (int unitNumber = firstUnitNumber; unitNumber < firstUnitNumber + quantity; unitNumber++) {
                        body.append("  Serial BH: BH-").append(order.getId()).append("-")
                                .append(productId).append("-").append(unitNumber).append("\n");
                    }
                    nextUnitByProduct.put(productId, firstUnitNumber + quantity - 1);
                }
            }

            body.append("\nTổng thanh toán: ")
                    .append(vnd.format(order.getTotalAmount().longValue())).append("đ\n")
                    .append("Đăng nhập để tra cứu thời hạn bảo hành trực tuyến tại: ")
                    .append(frontendUrl.replaceAll("/$", "")).append("/warranty.html\n\n")
                    .append("Trân trọng,\nĐội ngũ HK COMPUTER");

            sendBrevoEmail(toEmail.trim(),
                    "[HK COMPUTER] Xác nhận thanh toán & Kích hoạt bảo hành đơn #DH" + order.getId(),
                    body.toString());
        } catch (Exception e) {
            log.warn("Could not send order confirmation email for order #{}: {}", orderId, e.getMessage());
        }
    }

    private boolean hasValidBrevoConfig() {
        if (brevoApiKey == null || brevoApiKey.isBlank() || brevoFromEmail == null || brevoFromEmail.isBlank()) {
            return false;
        }
        return !brevoApiKey.trim().toLowerCase(Locale.ROOT).startsWith("dummy");
    }

    private void sendBrevoEmail(String to, String subject, String text) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        String jsonBody = """
                {
                  "sender": { "email": "%s", "name": "HK COMPUTER" },
                  "to": [ { "email": "%s" } ],
                  "subject": "%s",
                  "textContent": "%s"
                }
                """.formatted(escapeJson(brevoFromEmail), escapeJson(to), escapeJson(subject), escapeJson(text));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .timeout(Duration.ofSeconds(20))
                .header("api-key", brevoApiKey.trim())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Brevo order receipt HTTP {}: {}", response.statusCode(), response.body());
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
