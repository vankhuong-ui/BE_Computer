package com.example.MyProject.ORDER.service;

import com.example.MyProject.ORDER.dto.cart.CartRequest;
import com.example.MyProject.ORDER.dto.cart.CartResponse;

import java.util.List;

public interface CartService {

    String addCart(CartRequest cartRequest);

    void updateCart(Long productId, Integer quality);

    void deleteCart(Long productId);

    List<CartResponse> showCart();
}
