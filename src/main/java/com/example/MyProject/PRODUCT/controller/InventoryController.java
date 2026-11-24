package com.example.MyProject.PRODUCT.controller;

import com.example.MyProject.PRODUCT.common.ApiResponse;
import com.example.MyProject.PRODUCT.common.BaseController;
import com.example.MyProject.PRODUCT.common.ProcessPageable;
import com.example.MyProject.PRODUCT.dto.inventory.InventoryRequest;
import com.example.MyProject.PRODUCT.dto.inventory.InventoryResponse;
import com.example.MyProject.PRODUCT.dto.inventory.InventoryResponseUser;
import com.example.MyProject.PRODUCT.dto.inventory.InventoryTransferRequest;
import com.example.MyProject.PRODUCT.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory")
public class InventoryController extends BaseController {

    private final InventoryService inventoryService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<String> createInventory(@RequestBody InventoryRequest inventoryRequest) {
        inventoryService.createInventory(inventoryRequest);
        return successResponse("Khởi tạo / bổ sung kho thành công!");
    }

    // Nhập thêm hàng (Cộng dồn vào tồn hiện tại)
    @PutMapping("/{id}/soluong")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<String> updateSoluong(
            @PathVariable("id") Long id,
            @RequestParam("luongnhap") int luongnhap
    ) {
        inventoryService.updateSoluong(id, luongnhap);
        return successResponse("Đã nhập thêm " + luongnhap + " sản phẩm vào kho!");
    }

    // Kiểm kê / điều chỉnh tồn thực tế
    @PutMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<String> adjustSoluong(
            @PathVariable("id") Long id,
            @RequestParam("soluong") int exactSoluong
    ) {
        inventoryService.adjustSoluong(id, exactSoluong);
        return successResponse("Đã cập nhật số lượng kiểm kê thực tế!");
    }

    // Chuyển kho nội bộ giữa 2 cơ sở
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<String> transferStock(@RequestBody InventoryTransferRequest request) {
        inventoryService.transferStock(request);
        return successResponse("Chuyển kho giữa hai cơ sở thành công!");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Page<InventoryResponse>> showInventory(
            @RequestParam(name = "branch_id", required = false) Long branchId,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "stock_state", required = false) String stockState,
            @RequestParam(name = "page_no", defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "12") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = ProcessPageable.createPageable(page, size, sortBy, sortDir);
        Page<InventoryResponse> response = inventoryService.showInventory(branchId, city, keyword, stockState, pageable);
        return successDataResponse("Get inventory success", response);
    }

    @GetMapping("/search")
    public ApiResponse<InventoryResponse> searchInventory(
            @RequestParam("nameProduct") String nameProduct
    ) {
        InventoryResponse response = inventoryService.searchInventory(nameProduct);
        return successDataResponse("Search inventory success", response);
    }

    @GetMapping("/{id_product}/user")
    public ApiResponse<InventoryResponseUser> showInventoryUser(
            @PathVariable("id_product") Long id,
            @RequestParam(name = "branch_id", required = false) Long branchId,
            @RequestParam(name = "city", required = false) String city
    ) {
        return successDataResponse("Inventory success", inventoryService.detailInventory(id, branchId, city));
    }

    @GetMapping("/{id_product}/branches")
    public ApiResponse<List<InventoryResponse>> listProductBranches(@PathVariable("id_product") Long id) {
        return successDataResponse("Get branch stocks success", inventoryService.listBranchStocksByProduct(id));
    }
}
