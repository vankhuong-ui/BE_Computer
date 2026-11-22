package com.example.MyProject.REVIEW.controller;

import com.example.MyProject.ORDER.common.ApiResponse;
import com.example.MyProject.ORDER.common.BaseController;
import com.example.MyProject.REVIEW.dto.warranty.WarrantyCardResponse;
import com.example.MyProject.REVIEW.dto.warranty.WarrantyClaimRequest;
import com.example.MyProject.REVIEW.dto.warranty.WarrantyClaimResponse;
import com.example.MyProject.REVIEW.service.WarrantyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warranty")
public class WarrantyController extends BaseController {

    private final WarrantyService warrantyService;

    // 1. Tra cứu công khai phiếu bảo hành điện tử theo Mã Serial (BH-orderId-productId) hoặc Mã đơn hàng
    @GetMapping("/lookup")
    public ApiResponse<List<WarrantyCardResponse>> lookupWarranty(
            @RequestParam("keyword") String keyword
    ) {
        return successDataResponse("Tra cứu thông tin bảo hành thành công!", warrantyService.lookupWarranty(keyword));
    }

    // 2. Khách hàng xem danh sách thiết bị đang được bảo hành của mình
    @GetMapping("/my-cards")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_USER', 'ROLE_ADMIN')")
    public ApiResponse<List<WarrantyCardResponse>> getMyWarrantyCards() {
        return successDataResponse("Lấy danh sách phiếu bảo hành thành công!", warrantyService.getMyWarrantyCards());
    }

    // 3. Khách hàng gửi yêu cầu bảo hành / sửa chữa
    @PostMapping("/claims")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_USER', 'ROLE_ADMIN')")
    public ApiResponse<WarrantyClaimResponse> createClaim(
            @RequestBody WarrantyClaimRequest request
    ) {
        return successDataResponse("Gửi yêu cầu bảo hành thành công!", warrantyService.createClaim(request));
    }

    // 4. Khách hàng xem lịch sử yêu cầu bảo hành của mình
    @GetMapping("/my-claims")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'ROLE_USER', 'ROLE_ADMIN')")
    public ApiResponse<Page<WarrantyClaimResponse>> getMyClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return successDataResponse("Lấy lịch sử bảo hành thành công!", warrantyService.getMyClaims(pageable));
    }

    // 5. Admin xem toàn bộ danh sách yêu cầu bảo hành
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<Page<WarrantyClaimResponse>> getAllClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return successDataResponse("Lấy toàn bộ yêu cầu bảo hành thành công!", warrantyService.getAllClaims(pageable));
    }

    // 6. Admin cập nhật trạng thái xử lý & ghi chú kỹ thuật
    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ApiResponse<WarrantyClaimResponse> updateClaimStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "adminNote", required = false) String adminNote,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String note = adminNote;
        if ((note == null || note.isBlank()) && body != null && body.containsKey("adminNote")) {
            note = body.get("adminNote");
        }
        return successDataResponse("Cập nhật trạng thái bảo hành thành công!", warrantyService.updateClaimStatus(id, status, note));
    }
}
