package com.example.MyProject.REVIEW.repository;

import com.example.MyProject.REVIEW.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 1. Lấy tất cả bình luận công khai để hiển thị ở trang chi tiết sản phẩm
    Page<Review> findByProductId(Long productId, Pageable pageable);

    // 2. Check xem sản phẩm trong đơn hàng này đã từng được đánh giá chưa (tránh spam đánh giá lại)
    boolean existsByOrderIdAndProductId(Long orderId, Long productId);
    @Query("""
        SELECT AVG(r.rating)
        FROM Review r
        WHERE r.product.id = :productId
    """)
    Optional<Double> findAverageRatingByProductId(
            @Param("productId") Long productId
    );

    long countByProductId(Long productId);
}