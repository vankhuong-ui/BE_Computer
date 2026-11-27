package com.example.MyProject.REVIEW.service.impl;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.service.OrdersService;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.service.ProductService;
import com.example.MyProject.REVIEW.dto.review.CreateReviewRequest;
import com.example.MyProject.REVIEW.dto.review.ReviewResponse;
import com.example.MyProject.REVIEW.entity.Review;
import com.example.MyProject.REVIEW.mapper.ReviewMapper;
import com.example.MyProject.REVIEW.repository.ReviewRepository;
import com.example.MyProject.REVIEW.service.ReviewService;
import com.example.MyProject.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrdersService ordersService;
    private final ProductService productService;
    private final ReviewMapper reviewMapper;

    @Transactional
    @Override
    public void createReview(CreateReviewRequest request) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Orders order = ordersService.requireOwnedOrderWithDetails(request.orderId(), account.getId());

        if (!"PAID".equals(order.getStatus()) && !"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException("Chỉ được đánh giá sản phẩm sau khi đơn hàng đã thanh toán thành công");
        }

        boolean productInOrder = order.getOrderItems() != null && order.getOrderItems().stream()
                .anyMatch(item -> item.getProducts() != null && item.getProducts().getId().equals(request.productId()));
        if (!productInOrder) {
            throw new BusinessException("Sản phẩm không nằm trong đơn hàng này");
        }

        Products product = productService.requireProductById(request.productId());

        if (reviewRepository.existsByOrderIdAndProductId(request.orderId(), request.productId())) {
            throw new BusinessException("Sản phẩm trong đơn hàng này đã được đánh giá trước đó");
        }

        Review review = reviewMapper.toEntity(request);
        review.setOrder(order);
        review.setProduct(product);

        reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ReviewResponse> getReviewsByProductId(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable).map(reviewMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isReviewed(Long orderId, Long productId) {
        return reviewRepository.existsByOrderIdAndProductId(orderId, productId);
    }
}
