package com.example.MyProject.REVIEW.controller;

import com.example.MyProject.common.ApiResponse;
import com.example.MyProject.common.BaseController;
import com.example.MyProject.REVIEW.dto.repair.RepairBookingRequest;
import com.example.MyProject.REVIEW.dto.repair.RepairBookingResponse;
import com.example.MyProject.REVIEW.service.RepairBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/repairs")
public class RepairBookingController extends BaseController {

    private final RepairBookingService repairBookingService;

    // 1. Đặt lịch sửa chữa (Cho phép cả khách vãng lai lẫn tài khoản đã đăng nhập)
    @PostMapping
    public ApiResponse<RepairBookingResponse> createBooking(@RequestBody RepairBookingRequest request) {
        return successDataResponse("Đặt lịch sửa chữa thành công!", repairBookingService.createBooking(request));
    }

    // 2. Khách hàng xem lịch hẹn sửa chữa của mình
    @GetMapping("/my-bookings")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_USER', 'ROLE_ADMIN')")
    public ApiResponse<Page<RepairBookingResponse>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return successDataResponse("Lấy lịch hẹn sửa chữa thành công!", repairBookingService.getMyBookings(pageable));
    }

    // 3. Admin xem toàn bộ lịch hẹn sửa chữa
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<Page<RepairBookingResponse>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return successDataResponse("Lấy danh sách lịch hẹn thành công!", repairBookingService.getAllBookings(pageable));
    }

    // 4. Admin cập nhật trạng thái lịch hẹn & ghi chú kỹ thuật viên
    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<RepairBookingResponse> updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "adminNote", required = false) String adminNote,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String note = adminNote;
        if ((note == null || note.isBlank()) && body != null && body.containsKey("adminNote")) {
            note = body.get("adminNote");
        }
        return successDataResponse(
                "Cập nhật trạng thái lịch hẹn thành công!",
                repairBookingService.updateBookingStatus(id, status, note)
        );
    }
}
