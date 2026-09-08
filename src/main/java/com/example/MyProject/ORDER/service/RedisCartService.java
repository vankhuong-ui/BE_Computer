package com.example.MyProject.ORDER.service;

import java.util.Map;

public interface RedisCartService {

    void addProduct(Long accountId, Long productId, Integer quantity);

    void updateProduct(Long accountId, Long productId, Integer quantity);

    void deleteProduct(Long accountId, Long productId);

    Map<Object, Object> getCart(Long accountId);

    void clearCart(Long accountId);
}