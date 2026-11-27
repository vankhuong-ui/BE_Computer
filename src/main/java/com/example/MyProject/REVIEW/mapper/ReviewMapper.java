package com.example.MyProject.REVIEW.mapper;

import com.example.MyProject.REVIEW.dto.review.CreateReviewRequest;
import com.example.MyProject.REVIEW.dto.review.ReviewResponse;
import com.example.MyProject.REVIEW.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReviewMapper {

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "product.id", target = "productId")
    ReviewResponse toResponse(Review review);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "order", ignore = true)
    Review toEntity(CreateReviewRequest request);
}
