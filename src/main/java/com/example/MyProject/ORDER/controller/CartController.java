package com.example.MyProject.ORDER.controller;

import com.example.MyProject.ORDER.common.ApiResponse;
import com.example.MyProject.ORDER.common.BaseController;
import com.example.MyProject.ORDER.dto.cart.CartRequest;
import com.example.MyProject.ORDER.dto.cart.CartResponse;
import com.example.MyProject.ORDER.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController extends BaseController {

    private final CartService cartService;

    // Thêm sản phẩm vào Cart
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<String> addCart(
            @RequestBody CartRequest request
    ) {

        return successResponse(
                cartService.addCart(request)
        );
    }

    // Update số lượng
    @PutMapping("/{productId}/update")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<String> updateCart(
            @PathVariable("productId") Long productId,
            @RequestParam("quality") Integer quality
    ) {

        cartService.updateCart(
                productId,
                quality
        );

        return successResponse(
                "Update cart success!"
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<String> deleteCart(
            @PathVariable("id") Long id
    ) {
        cartService.deleteCart(id);

        return successResponse(
                "Delete cart success!"
        );
    }

    // Xem Cart của mình
    @GetMapping("/my-cart")
    public ApiResponse<List<CartResponse>> showCart() {

        return successDataResponse(
                "Get my cart success",
                cartService.showCart()
        );
    }
}