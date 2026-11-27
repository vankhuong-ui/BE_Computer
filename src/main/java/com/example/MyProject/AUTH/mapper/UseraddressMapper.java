package com.example.MyProject.AUTH.mapper;

import com.example.MyProject.AUTH.dto.address.request.UseraddressRequest;
import com.example.MyProject.AUTH.dto.address.response.UseraddressResponse;
import com.example.MyProject.AUTH.entity.Useraddresses;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UseraddressMapper {

    @Mapping(target = "account", ignore = true)
    Useraddresses toEntity(UseraddressRequest useraddressRequest);
    @Mapping(source = "account.id", target = "accountId")
    UseraddressResponse toRespon(Useraddresses useraddresses);
    @Mapping(target = "account", ignore = true)
    void updateUseraddress(UseraddressRequest useraddressRequest, @MappingTarget Useraddresses useraddresses);
}
