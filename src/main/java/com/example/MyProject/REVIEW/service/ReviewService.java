package com.example.MyProject.REVIEW.service;


import com.example.MyProject.REVIEW.dto.review.CreateReviewRequest;
import com.example.MyProject.REVIEW.dto.review.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    void createReview(CreateReviewRequest request);
    Page<ReviewResponse> getReviewsByProductId(Long productId, Pageable pageable);
    boolean isReviewed(Long orderId, Long productId);
}
