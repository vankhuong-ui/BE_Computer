package com.example.MyProject.REVIEW.controller;

import com.example.MyProject.ORDER.common.ApiResponse;
import com.example.MyProject.ORDER.common.BaseController;
import com.example.MyProject.REVIEW.dto.review.CreateReviewRequest;
import com.example.MyProject.REVIEW.dto.review.ReviewResponse;
import com.example.MyProject.REVIEW.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController extends BaseController {

    private final ReviewService reviewService;

    // 1. Tạo đánh giá (Cần Token User - Bắt buộc truyền productId và orderId để kiểm tra)
    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ROLE_USER')")
    public ApiResponse<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request) {
        reviewService.createReview(request);
        return successResponse("Đánh giá sản phẩm thành công!");
    }

    // 2. Đọc bình luận theo Product ID (Public - Cho mọi người xem)
    @GetMapping("/product/{product_id}")
    public ApiResponse<Page<ReviewResponse>> getReviewsByProduct(
            @PathVariable("product_id") Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return successDataResponse("Lấy danh sách đánh giá thành công!", reviewService.getReviewsByProductId(productId, pageable));
    }
    @GetMapping("/check")
    @PreAuthorize("hasAnyAuthority('USER', 'ROLE_USER')")
    public ApiResponse<Boolean> checkReviewed(
            @RequestParam Long orderId,
            @RequestParam Long productId
    ) {
        boolean reviewed = reviewService.isReviewed(orderId, productId);
        return successDataResponse("Check review status", reviewed);
    }
}
