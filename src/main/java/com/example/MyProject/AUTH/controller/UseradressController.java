package com.example.MyProject.AUTH.controller;

import com.example.MyProject.AUTH.dto.address.request.UseraddressRequest;
import com.example.MyProject.AUTH.dto.address.response.UseraddressResponse;
import com.example.MyProject.AUTH.service.UseraddressService;
import com.example.MyProject.common.ApiResponse;
import com.example.MyProject.common.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/useraddresses")
public class UseradressController extends BaseController {

    private final UseraddressService useraddressService;

    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<List<UseraddressResponse>> listAddressesOfUser() {
        return successDataResponse("List Addresses User", useraddressService.listaddress());
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<String> createAddresses(@RequestBody UseraddressRequest useraddressRequest) {
        useraddressService.createUseraddress(useraddressRequest);
        return successResponse("Create Addresses success");
    }

    @PutMapping("/{id}/update")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<String> updateAddresses(
            @PathVariable("id") Long id,
            @RequestBody UseraddressRequest useraddressRequest
    ) {
        useraddressService.updateUseraddress(id, useraddressRequest);
        return successResponse("Update Addressess success");
    }

    @DeleteMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('USER')")
    public ApiResponse<String> deleteAddresses(@PathVariable("id") Long id) {
        useraddressService.deleteUseraddress(id);
        return successResponse("Delete Addresses success");
    }
}
