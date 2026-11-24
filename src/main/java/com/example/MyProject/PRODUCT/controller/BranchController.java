package com.example.MyProject.PRODUCT.controller;

import com.example.MyProject.PRODUCT.common.ApiResponse;
import com.example.MyProject.PRODUCT.common.BaseController;
import com.example.MyProject.PRODUCT.dto.branch.BranchRequest;
import com.example.MyProject.PRODUCT.dto.branch.BranchResponse;
import com.example.MyProject.PRODUCT.entity.Branch;
import com.example.MyProject.PRODUCT.entity.Inventory;
import com.example.MyProject.PRODUCT.repository.BranchRepository;
import com.example.MyProject.PRODUCT.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/branches")
public class BranchController extends BaseController {

    private final BranchRepository branchRepository;
    private final InventoryRepository inventoryRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<BranchResponse>> listPublicBranches(
            @RequestParam(name = "city", required = false) String city
    ) {
        List<Branch> branches = (city != null && !city.isBlank())
                ? branchRepository.findByActiveTrueAndCityIgnoreCaseOrderByIdAsc(city.trim())
                : branchRepository.findByActiveTrueOrderByIdAsc();
        return successDataResponse("Get branches success", toBranchResponses(branches));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Transactional(readOnly = true)
    public ApiResponse<List<BranchResponse>> listAllAdminBranches() {
        List<Branch> branches = branchRepository.findAllByOrderByIdAsc();
        return successDataResponse("Get all branches success", toBranchResponses(branches));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Transactional
    public ApiResponse<BranchResponse> createBranch(@RequestBody BranchRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new RuntimeException("Tên cơ sở không được để trống");
        }
        if (req.city() == null || req.city().isBlank()) {
            throw new RuntimeException("Tỉnh/Thành phố không được để trống");
        }
        String code = (req.code() != null && !req.code().isBlank())
                ? req.code().trim().toUpperCase()
                : "BR-" + System.currentTimeMillis();
        if (branchRepository.existsByCodeIgnoreCase(code)) {
            throw new RuntimeException("Mã cơ sở " + code + " đã tồn tại");
        }

        Branch b = new Branch();
        b.setCode(code);
        b.setName(req.name().trim());
        b.setCity(req.city().trim());
        b.setDistrict(req.district() != null ? req.district().trim() : "");
        b.setAddress(req.address() != null ? req.address().trim() : "");
        b.setPhone(req.phone() != null ? req.phone().trim() : "");
        b.setOpeningHours(req.openingHours() != null && !req.openingHours().isBlank() ? req.openingHours().trim() : "8:00 - 21:00");
        b.setServices(req.services() != null && !req.services().isBlank() ? req.services().trim() : "Bán hàng · Sửa chữa · Bảo hành");
        b.setActive(req.active() == null || req.active());

        Branch saved = branchRepository.save(b);
        return successDataResponse("Tạo cơ sở thành công", toBranchResponses(List.of(saved)).get(0));
    }

    @PutMapping("/{id}/update")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Transactional
    public ApiResponse<BranchResponse> updateBranch(@PathVariable("id") Long id, @RequestBody BranchRequest req) {
        Branch b = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ sở #" + id));

        if (req.code() != null && !req.code().isBlank()) {
            String newCode = req.code().trim().toUpperCase();
            branchRepository.findByCodeIgnoreCase(newCode).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new RuntimeException("Mã cơ sở " + newCode + " đã được sử dụng");
                }
            });
            b.setCode(newCode);
        }
        if (req.name() != null && !req.name().isBlank()) b.setName(req.name().trim());
        if (req.city() != null && !req.city().isBlank()) b.setCity(req.city().trim());
        if (req.district() != null) b.setDistrict(req.district().trim());
        if (req.address() != null && !req.address().isBlank()) b.setAddress(req.address().trim());
        if (req.phone() != null) b.setPhone(req.phone().trim());
        if (req.openingHours() != null && !req.openingHours().isBlank()) b.setOpeningHours(req.openingHours().trim());
        if (req.services() != null) b.setServices(req.services().trim());
        if (req.active() != null) b.setActive(req.active());

        Branch saved = branchRepository.save(b);
        return successDataResponse("Cập nhật cơ sở thành công", toBranchResponses(List.of(saved)).get(0));
    }

    @DeleteMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Transactional
    public ApiResponse<String> deleteBranch(@PathVariable("id") Long id) {
        Branch b = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ sở #" + id));
        long stock = inventoryRepository.findByBranch_Id(id).stream()
                .mapToLong(inv -> Math.max(0, inv.getSoluong()))
                .sum();
        if (stock > 0) {
            throw new RuntimeException("Cơ sở vẫn còn " + stock + " sản phẩm tồn kho. Vui lòng chuyển kho hoặc đưa tồn về 0 trước khi xóa (hoặc chuyển sang Tạm ngưng).");
        }
        branchRepository.delete(b);
        return successResponse("Đã xóa cơ sở");
    }

    private List<BranchResponse> toBranchResponses(List<Branch> branches) {
        if (branches.isEmpty()) return List.of();
        List<Long> branchIds = branches.stream().map(Branch::getId).toList();
        List<Inventory> inventories = inventoryRepository.findByBranch_IdIn(branchIds);

        Map<Long, List<Inventory>> invByBranch = inventories.stream()
                .filter(i -> i.getBranch() != null && i.getBranch().getId() != null)
                .collect(Collectors.groupingBy(i -> i.getBranch().getId()));

        return branches.stream().map(b -> {
            List<Inventory> list = invByBranch.getOrDefault(b.getId(), List.of());
            long productCount = list.stream().filter(i -> i.getSoluong() > 0).count();
            long totalStock = list.stream().mapToLong(i -> Math.max(0, i.getSoluong())).sum();
            return new BranchResponse(
                    b.getId(),
                    b.getCode(),
                    b.getName(),
                    b.getCity(),
                    b.getDistrict(),
                    b.getAddress(),
                    b.getPhone(),
                    b.getOpeningHours(),
                    b.getServices(),
                    b.isActive(),
                    productCount,
                    totalStock
            );
        }).toList();
    }
}
