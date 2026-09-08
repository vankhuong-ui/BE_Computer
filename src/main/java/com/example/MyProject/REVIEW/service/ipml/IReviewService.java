package com.example.MyProject.REVIEW.service.ipml;

import com.example.MyProject.AUTH.entity.Account;

import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.repository.ProductRepository;

import com.example.MyProject.REVIEW.dto.review.CreateReviewRequest;
import com.example.MyProject.REVIEW.dto.review.ReviewResponse;
import com.example.MyProject.REVIEW.entity.Review;
import com.example.MyProject.REVIEW.mapper.ReviewMapper;
import com.example.MyProject.REVIEW.repository.ReviewRepository;
import com.example.MyProject.REVIEW.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IReviewService implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrdersRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReviewMapper reviewMapper;

    @Transactional
    @Override
    public void createReview(CreateReviewRequest request) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 1. Kiểm tra đơn hàng có tồn tại không
        Orders order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

   //     Account account1= (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();


        // 3. Validation 2: Đơn hàng phải đã thanh toán / hoàn thành (PAID hoặc COMPLETED)
        if (!"UNPAID".equals(order.getStatus()) && !"COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("Chỉ được đánh giá sản phẩm sau khi đơn hàng đã thanh toán thành công");
        }

        // 4. Kiểm tra sản phẩm có tồn tại không
        Products product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // 5. Validation 3: Tránh việc 1 sản phẩm trong 1 đơn hàng bị đánh giá lại nhiều lần
        if (reviewRepository.existsByOrderIdAndProductId(request.orderId(), request.productId())) {
            throw new RuntimeException("Sản phẩm trong đơn hàng này đã được đánh giá trước đó");
        }

        Review review = reviewMapper.toEntity(request);
        review.setOrder(order);
        review.setProduct(product);

        Review savedReview = reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ReviewResponse> getReviewsByProductId(Long productId, Pageable pageable) {
        // Lấy danh sách bình luận theo sản phẩm để hiển thị công khai ở trang sản phẩm
        return reviewRepository.findByProductId(productId, pageable)
                .map(reviewMapper::toResponse);
    }
    @Override
    public boolean isReviewed(Long orderId, Long productId) {
        return reviewRepository.existsByOrderIdAndProductId(orderId, productId);
    }
}
