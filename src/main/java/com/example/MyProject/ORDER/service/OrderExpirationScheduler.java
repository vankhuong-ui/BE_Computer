package com.example.MyProject.ORDER.service;

import com.example.MyProject.ORDER.repository.OrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Periodically cancels stale UNPAID orders to release reserved branch inventory and voucher usage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrdersRepository ordersRepository;
    private final OrdersService ordersService;

    @Value("${orders.expiration.minutes:30}")
    private long expirationMinutes = 30;

    @Scheduled(fixedDelayString = "${orders.expiration.scan-ms:300000}", initialDelayString = "${orders.expiration.initial-delay-ms:60000}")
    public int cancelExpiredUnpaidOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(Math.max(5, expirationMinutes));
        List<Long> expiredIds = ordersRepository.findExpiredUnpaidOrderIds(cutoff);
        if (expiredIds == null || expiredIds.isEmpty()) {
            return 0;
        }

        int cancelled = 0;
        for (Long orderId : expiredIds) {
            try {
                ordersService.updateStatus(orderId, "CANCELLED");
                cancelled++;
                log.info("Auto-cancelled expired UNPAID order #{} (older than {}m)", orderId, expirationMinutes);
            } catch (Exception e) {
                log.warn("Failed to auto-cancel expired order #{}: {}", orderId, e.getMessage());
            }
        }
        return cancelled;
    }
}
