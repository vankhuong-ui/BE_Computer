package com.example.MyProject.REVIEW.mapper;


import com.example.MyProject.REVIEW.dto.support.SupportResponse;
import com.example.MyProject.REVIEW.entity.Support;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SupportMapper {

    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "account.username", target = "username")
    SupportResponse toResponse(Support support);
}