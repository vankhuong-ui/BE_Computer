package com.example.MyProject.PRODUCT.controller;

import com.example.MyProject.PRODUCT.common.ApiResponse;
import com.example.MyProject.PRODUCT.common.BaseController;
import com.example.MyProject.PRODUCT.dto.branch.BranchRequest;
import com.example.MyProject.PRODUCT.dto.branch.BranchResponse;
import com.example.MyProject.PRODUCT.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/branches")
public class BranchController extends BaseController {

    private final BranchService branchService;

    @GetMapping
    public ApiResponse<List<BranchResponse>> listPublicBranches(
            @RequestParam(name = "city", required = false) String city
    ) {
        return successDataResponse("Get branches success", branchService.listPublicBranches(city));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<List<BranchResponse>> listAllAdminBranches() {
        return successDataResponse("Get all branches success", branchService.listAllAdminBranches());
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<BranchResponse> createBranch(@RequestBody BranchRequest req) {
        return successDataResponse("Tạo cơ sở thành công", branchService.createBranch(req));
    }

    @PutMapping("/{id}/update")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<BranchResponse> updateBranch(
            @PathVariable("id") Long id,
            @RequestBody BranchRequest req
    ) {
        return successDataResponse("Cập nhật cơ sở thành công", branchService.updateBranch(id, req));
    }

    @DeleteMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<String> deleteBranch(@PathVariable("id") Long id) {
        branchService.deleteBranch(id);
        return successResponse("Đã xóa cơ sở");
    }
}
