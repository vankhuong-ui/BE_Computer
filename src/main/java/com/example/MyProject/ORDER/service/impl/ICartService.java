package com.example.MyProject.ORDER.service.impl;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.ORDER.dto.cart.CartRequest;
import com.example.MyProject.ORDER.dto.cart.CartResponse;
import com.example.MyProject.ORDER.entity.Cart;
import com.example.MyProject.ORDER.mapper.CartMapper;
import com.example.MyProject.ORDER.repository.CartRepository;
import com.example.MyProject.ORDER.service.CartService;
import com.example.MyProject.ORDER.service.RedisCartService;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.mapper.ProductMapper;
import com.example.MyProject.PRODUCT.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ICartService implements CartService {

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final RedisCartService redisCartService;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    private Account getCurrentAccount() {

        return (Account) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    // =========================
    // ADD CART
    // =========================

    @Override
    @Transactional
    public String addCart(CartRequest cartRequest) {

        Account account = getCurrentAccount();

        if (cartRequest.productId() == null) {
            throw new RuntimeException("ProductId không được null");
        }

        if (cartRequest.quality() == null || cartRequest.quality() <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        Products product = productRepository
                .findById(cartRequest.productId())
                .orElseThrow(() ->
                        new RuntimeException("Product không tồn tại")
                );

        // Tìm Cart trong DB
        Cart cart = cartRepository.findByAccountIdAndProductsId(
                account.getId(),
                product.getId()
        );

        if (cart != null) {

            // Đã có → cộng số lượng
            cart.setQuality(
                    cart.getQuality() + cartRequest.quality()
            );

            cartRepository.save(cart);

        } else {

            // Chưa có → tạo mới
            Cart newCart = cartMapper.toEntity(
                    cartRequest,
                    account.getId()
            );

            cartRepository.save(newCart);
        }

        // DB thành công → cập nhật Redis
        redisCartService.addProduct(
                account.getId(),
                product.getId(),
                cartRequest.quality()
        );

        return "Thêm vào Cart thành công";
    }

    // =========================
    // UPDATE CART
    // =========================

    @Override
    @Transactional
    public void updateCart(Long productId, Integer quality) {

        Account account = getCurrentAccount();

        if (quality == null || quality <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        Products product = productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product không tồn tại")
                );

        Cart cart = cartRepository.findByAccountIdAndProductsId(
                account.getId(),
                product.getId()
        );

        if (cart == null) {
            throw new RuntimeException(
                    "Sản phẩm không có trong giỏ hàng"
            );
        }

        // Update DB
        cart.setQuality(quality);
        cartRepository.save(cart);

        // DB thành công → update Redis
        redisCartService.updateProduct(
                account.getId(),
                productId,
                quality
        );
    }

    // =========================
    // DELETE CART
    // =========================

    @Override
    @Transactional
    public void deleteCart(Long cartId) {

        Account account = getCurrentAccount();

        // Tìm Cart theo ID
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() ->
                        new RuntimeException("Cart không tồn tại")
                );

        // Kiểm tra Cart có thuộc user đang đăng nhập không
        if (!cart.getAccount().getId().equals(account.getId())) {
            throw new RuntimeException(
                    "Bạn không thể xóa Cart của người khác"
            );
        }

        // Lấy productId trước khi xóa
        Long productId = cart.getProducts().getId();

        // Xóa DB
        cartRepository.delete(cart);

        // Xóa khỏi Redis
        redisCartService.deleteProduct(
                account.getId(),
                productId
        );
    }

    // =========================
    // SHOW CART
    // =========================

    @Override
    public List<CartResponse> showCart() {

        Account account = getCurrentAccount();

        // 1. Lấy từ Redis
        Map<Object, Object> redisCart =
                redisCartService.getCart(account.getId());

        // Redis có dữ liệu
        if (!redisCart.isEmpty()) {

            return convertRedisCart(redisCart);
        }

        // 2. Redis không có → lấy DB
        List<Cart> carts =
                cartRepository.findByAccountId(account.getId());

        List<CartResponse> result = new ArrayList<>();

        for (Cart cart : carts) {

            result.add(
                    new CartResponse(
                            cart.getId(),
                            productMapper.toReponse(
                                    cart.getProducts()
                            ),
                            cart.getQuality()
                    )
            );

            // Đưa dữ liệu DB vào Redis
            redisCartService.addProduct(
                    account.getId(),
                    cart.getProducts().getId(),
                    cart.getQuality()
            );
        }

        return result;
    }

    // =========================
    // REDIS → RESPONSE
    // =========================

    private List<CartResponse> convertRedisCart(
            Map<Object, Object> redisCart
    ) {

        List<CartResponse> result = new ArrayList<>();

        for (Map.Entry<Object, Object> entry :
                redisCart.entrySet()) {

            Long productId =
                    Long.parseLong(entry.getKey().toString());

            Integer quantity =
                    Integer.parseInt(entry.getValue().toString());

            Products product =
                    productRepository.findById(productId)
                            .orElse(null);

            if (product == null) {
                continue;
            }

            // Lấy Cart ID từ DB
            Account account = getCurrentAccount();

            Cart cart =
                    cartRepository.findByAccountIdAndProductsId(
                            account.getId(),
                            productId
                    );

            if (cart == null) {
                continue;
            }

            result.add(
                    new CartResponse(
                            cart.getId(),
                            productMapper.toReponse(product),
                            quantity
                    )
            );
        }

        return result;
    }
}