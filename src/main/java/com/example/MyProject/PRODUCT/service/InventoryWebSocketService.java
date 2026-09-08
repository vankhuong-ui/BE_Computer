package com.example.MyProject.PRODUCT.service;

import com.example.MyProject.PRODUCT.dto.inventory.InventoryWebSocketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendStockUpdate(
            Long inventoryId,
            Long productId,
            Integer stockQuantity) {

        InventoryWebSocketResponse response =
                new InventoryWebSocketResponse(
                        inventoryId,
                        productId,
                        stockQuantity
                );

        messagingTemplate.convertAndSend(
                "/topic/inventory/" + inventoryId,
                response
        );
    }
}