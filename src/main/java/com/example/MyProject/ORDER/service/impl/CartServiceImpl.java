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
import com.example.MyProject.PRODUCT.service.ProductService;
import com.example.MyProject.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final RedisCartService redisCartService;
    private final ProductService productService;

    private Account getCurrentAccount() {
        return (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Override
    @Transactional
    public String addCart(CartRequest cartRequest) {
        Account account = getCurrentAccount();

        if (cartRequest.productId() == null) {
            throw new BusinessException("ProductId không được null");
        }
        if (cartRequest.quality() == null || cartRequest.quality() <= 0) {
            throw new BusinessException("Số lượng phải lớn hơn 0");
        }

        Products product = productService.requireProductById(cartRequest.productId());

        Cart cart = cartRepository.findByAccountIdAndProductsId(account.getId(), product.getId());
        if (cart != null) {
            cart.setQuality(cart.getQuality() + cartRequest.quality());
            cartRepository.save(cart);
        } else {
            Cart newCart = cartMapper.toEntity(cartRequest, account.getId());
            cartRepository.save(newCart);
        }

        redisCartService.addProduct(account.getId(), product.getId(), cartRequest.quality());
        return "Thêm vào Cart thành công";
    }

    @Override
    @Transactional
    public void updateCart(Long productId, Integer quality) {
        Account account = getCurrentAccount();

        if (quality == null || quality <= 0) {
            throw new BusinessException("Số lượng phải lớn hơn 0");
        }

        Products product = productService.requireProductById(productId);
        Cart cart = cartRepository.findByAccountIdAndProductsId(account.getId(), product.getId());
        if (cart == null) {
            throw new BusinessException("Sản phẩm không có trong giỏ hàng");
        }

        cart.setQuality(quality);
        cartRepository.save(cart);
        redisCartService.updateProduct(account.getId(), productId, quality);
    }

    @Override
    @Transactional
    public void deleteCart(Long cartId) {
        Account account = getCurrentAccount();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new BusinessException("Giỏ hàng không tồn tại"));

        if (!cart.getAccount().getId().equals(account.getId())) {
            throw new SecurityException("Bạn không thể xóa giỏ hàng của người khác");
        }

        Long productId = cart.getProducts().getId();
        cartRepository.delete(cart);
        redisCartService.deleteProduct(account.getId(), productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartResponse> showCart() {
        Account account = getCurrentAccount();

        Map<Object, Object> redisCart = redisCartService.getCart(account.getId());
        if (!redisCart.isEmpty()) {
            return convertRedisCart(account.getId(), redisCart);
        }

        List<Cart> carts = cartRepository.findByAccountId(account.getId());
        List<CartResponse> result = new ArrayList<>();
        for (Cart cart : carts) {
            result.add(new CartResponse(
                    cart.getId(),
                    productService.toProductResponse(cart.getProducts()),
                    cart.getQuality()
            ));
            redisCartService.addProduct(account.getId(), cart.getProducts().getId(), cart.getQuality());
        }
        return result;
    }

    private List<CartResponse> convertRedisCart(Long accountId, Map<Object, Object> redisCart) {
        List<Cart> userCarts = cartRepository.findByAccountId(accountId);
        Map<Long, Cart> cartByProductId = userCarts.stream()
                .collect(Collectors.toMap(
                        c -> c.getProducts().getId(),
                        c -> c,
                        (existing, replacement) -> existing
                ));

        List<CartResponse> result = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : redisCart.entrySet()) {
            Long productId = Long.parseLong(entry.getKey().toString());
            Integer quantity = Integer.parseInt(entry.getValue().toString());

            Cart cart = cartByProductId.get(productId);
            if (cart == null || cart.getProducts() == null) {
                continue;
            }

            result.add(new CartResponse(
                    cart.getId(),
                    productService.toProductResponse(cart.getProducts()),
                    quantity
            ));
        }
        return result;
    }
}
