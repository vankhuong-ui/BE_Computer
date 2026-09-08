package com.example.MyProject.ORDER.service.impl;

import com.example.MyProject.ORDER.service.RedisCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class IRedisCartService implements RedisCartService {

    private final StringRedisTemplate redisTemplate;

    private String getCartKey(Long accountId) {
        return "cart:" + accountId;
    }

    @Override
    public void addProduct(Long accountId, Long productId, Integer quantity) {

        String key = getCartKey(accountId);

        Object oldValue = redisTemplate.opsForHash()
                .get(key, productId.toString());

        if (oldValue != null) {

            int oldQuantity = Integer.parseInt(oldValue.toString());

            redisTemplate.opsForHash().put(
                    key,
                    productId.toString(),
                    String.valueOf(oldQuantity + quantity)
            );

        } else {

            redisTemplate.opsForHash().put(
                    key,
                    productId.toString(),
                    String.valueOf(quantity)
            );
        }
    }

    @Override
    public void updateProduct(
            Long accountId,
            Long productId,
            Integer quantity
    ) {

        String key = getCartKey(accountId);

        redisTemplate.opsForHash().put(
                key,
                productId.toString(),
                String.valueOf(quantity)
        );
    }

    @Override
    public void deleteProduct(
            Long accountId,
            Long productId
    ) {

        String key = getCartKey(accountId);

        redisTemplate.opsForHash()
                .delete(key, productId.toString());
    }

    @Override
    public Map<Object, Object> getCart(Long accountId) {

        return redisTemplate.opsForHash()
                .entries(getCartKey(accountId));
    }

    @Override
    public void clearCart(Long accountId) {

        redisTemplate.delete(getCartKey(accountId));
    }
}