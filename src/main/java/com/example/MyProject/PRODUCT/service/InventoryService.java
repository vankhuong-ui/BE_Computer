package com.example.MyProject.PRODUCT.service;

import com.example.MyProject.PRODUCT.dto.inventory.InventoryRequest;
import com.example.MyProject.PRODUCT.dto.inventory.InventoryResponse;
import com.example.MyProject.PRODUCT.dto.inventory.InventoryResponseUser;
import com.example.MyProject.PRODUCT.dto.inventory.InventoryTransferRequest;
import com.example.MyProject.PRODUCT.entity.Branch;
import com.example.MyProject.PRODUCT.entity.Inventory;
import com.example.MyProject.PRODUCT.entity.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService {
    void createInventory(InventoryRequest inventoryRequest);
    void updateSoluong(Long id, int luongnhap);
    void adjustSoluong(Long id, int exactSoluong);
    void transferStock(InventoryTransferRequest request);
    Page<InventoryResponse> showInventory(Long branchId, String city, String keyword, String stockState, Pageable pageable);
    InventoryResponse searchInventory(String nameProduct);
    InventoryResponseUser detailInventory(Long productId, Long branchId, String city);
    List<InventoryResponse> listBranchStocksByProduct(Long productId);

    Branch allocateStockForOrder(
            Products product,
            int quantity,
            boolean isStorePickup,
            Branch pickupBranch,
            Long preferredBranchId,
            String preferredCity,
            String shippingAddressText
    );
    void restoreStock(Long productId, Long branchId, int quantity);
    List<Inventory> findInventoriesByProductIds(List<Long> productIds);
    List<Inventory> findAllInventories();
}
