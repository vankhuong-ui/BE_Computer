package com.example.MyProject.REVIEW.controller;

import com.example.MyProject.ORDER.common.ApiResponse;
import com.example.MyProject.ORDER.common.BaseController;

import com.example.MyProject.REVIEW.dto.support.SupportResponse;
import com.example.MyProject.REVIEW.entity.Support;
import com.example.MyProject.REVIEW.service.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/support")
public class SupportController extends BaseController {

    private final SupportService supportService;

    // 1. Tạo yêu cầu mới (Trả về thông báo thành công, không trả data)
    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ROLE_USER')")
    public ApiResponse<Void> createRequest(@RequestBody Support support) {
        supportService.createSupportRequest(support);
        return successResponse("Gửi yêu cầu hỗ trợ thành công!");
    }

    // 2. User xem lại hỗ trợ của mình
    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyAuthority('USER', 'ROLE_USER')")
    public ApiResponse<Page<SupportResponse>> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return successDataResponse("Lấy danh sách thành công!", supportService.getMySupportRequests(pageable));
    }

    // 3. Admin xem tất cả các yêu cầu hỗ trợ trong hệ thống
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<Page<SupportResponse>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return successDataResponse("Lấy toàn bộ yêu cầu hỗ trợ thành công!", supportService.getAllSupportRequests(pageable));
    }

    // 4. Admin đổi status (Trả về thông báo thành công, không trả data)
    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<Void> updateStatus(
            @PathVariable("id") Long id,
            @RequestParam String status
    ) {
        supportService.updateSupportStatus(id, status);
        return successResponse("Cập nhật trạng thái thành công!");
    }
}