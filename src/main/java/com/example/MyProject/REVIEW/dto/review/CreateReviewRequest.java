package com.example.MyProject.REVIEW.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateReviewRequest(
        @NotNull(message = "ID đơn hàng không được để trống")
        Long orderId,

        @NotNull(message = "ID sản phẩm không được để trống")
        Long productId,

        @NotNull(message = "Số sao đánh giá không được để trống")
        @Min(value = 1, message = "Đánh giá tối thiểu là 1 sao")
        @Max(value = 5, message = "Đánh giá tối đa là 5 sao")
        Integer rating,

        String comment
) {}