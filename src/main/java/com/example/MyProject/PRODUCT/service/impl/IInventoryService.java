package com.example.MyProject.PRODUCT.service.impl;

import com.example.MyProject.PRODUCT.dto.inventory.InventoryRequest;
import com.example.MyProject.PRODUCT.dto.inventory.InventoryResponse;
import com.example.MyProject.PRODUCT.dto.inventory.InventoryResponseUser;
import com.example.MyProject.PRODUCT.dto.inventory.InventoryTransferRequest;
import com.example.MyProject.PRODUCT.entity.Branch;
import com.example.MyProject.PRODUCT.entity.Inventory;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.mapper.InventoryMapper;
import com.example.MyProject.PRODUCT.repository.BranchRepository;
import com.example.MyProject.PRODUCT.repository.InventoryRepository;
import com.example.MyProject.PRODUCT.repository.ProductRepository;
import com.example.MyProject.PRODUCT.service.InventoryService;
import com.example.MyProject.PRODUCT.service.InventoryWebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IInventoryService implements InventoryService {

    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryWebSocketService inventoryWebSocketService;

    @Override
    @Transactional
    public void createInventory(InventoryRequest req) {
        if (req.productId() == null) {
            throw new RuntimeException("Vui lòng chọn sản phẩm");
        }
        if (req.soluong() < 0) {
            throw new RuntimeException("Số lượng tồn kho không được âm");
        }

        Products product = productRepository.findById(req.productId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        if (Boolean.TRUE.equals(req.allBranches())) {
            List<Branch> branches = branchRepository.findByActiveTrueOrderByIdAsc();
            if (branches.isEmpty()) {
                throw new RuntimeException("Chưa có cơ sở nào đang hoạt động");
            }
            for (Branch b : branches) {
                Optional<Inventory> existing = inventoryRepository.findByProducts_IdAndBranch_Id(product.getId(), b.getId());
                if (existing.isPresent()) {
                    Inventory inv = existing.get();
                    inv.setSoluong(inv.getSoluong() + req.soluong());
                    inventoryRepository.save(inv);
                } else {
                    Inventory inv = new Inventory();
                    inv.setProducts(product);
                    inv.setBranch(b);
                    inv.setSoluong(req.soluong());
                    inv.setSelled(0);
                    inventoryRepository.save(inv);
                }
            }
            broadcastProductTotalStock(product.getId());
            return;
        }

        Branch branch;
        if (req.branchId() != null) {
            branch = branchRepository.findById(req.branchId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ sở #" + req.branchId()));
        } else {
            branch = branchRepository.findAllByOrderByIdAsc().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("Hệ thống chưa có cơ sở nào"));
        }

        Optional<Inventory> existing = inventoryRepository.findByProducts_IdAndBranch_Id(product.getId(), branch.getId());
        if (existing.isPresent()) {
            Inventory inv = existing.get();
            inv.setSoluong(inv.getSoluong() + req.soluong());
            inventoryRepository.save(inv);
            broadcastProductTotalStock(product.getId());
            return;
        }

        Inventory inv = new Inventory();
        inv.setProducts(product);
        inv.setBranch(branch);
        inv.setSoluong(req.soluong());
        inv.setSelled(0);
        inventoryRepository.save(inv);
        broadcastProductTotalStock(product.getId());
    }

    @Override
    @Transactional
    public void updateSoluong(Long id, int luongnhap) {
        if (luongnhap <= 0) {
            throw new RuntimeException("Số lượng nhập thêm phải lớn hơn 0");
        }
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bản ghi tồn kho không tồn tại"));
        inventory.setSoluong(Math.max(0, inventory.getSoluong()) + luongnhap);
        inventoryRepository.save(inventory);

        broadcastProductTotalStock(inventory.getProducts().getId());
    }

    @Override
    @Transactional
    public void adjustSoluong(Long id, int exactSoluong) {
        if (exactSoluong < 0) {
            throw new RuntimeException("Số lượng kiểm kê thực tế không được âm");
        }
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bản ghi tồn kho không tồn tại"));
        inventory.setSoluong(exactSoluong);
        inventoryRepository.save(inventory);

        broadcastProductTotalStock(inventory.getProducts().getId());
    }

    @Override
    @Transactional
    public void transferStock(InventoryTransferRequest req) {
        if (req.productId() == null || req.fromBranchId() == null || req.toBranchId() == null) {
            throw new RuntimeException("Vui lòng chọn đầy đủ sản phẩm, cơ sở xuất và cơ sở nhận");
        }
        if (req.fromBranchId().equals(req.toBranchId())) {
            throw new RuntimeException("Cơ sở xuất và cơ sở nhận phải khác nhau");
        }
        if (req.quantity() <= 0) {
            throw new RuntimeException("Số lượng chuyển kho phải lớn hơn 0");
        }

        Inventory source = inventoryRepository.findByProducts_IdAndBranch_Id(req.productId(), req.fromBranchId())
                .orElseThrow(() -> new RuntimeException("Cơ sở xuất chưa có kho cho sản phẩm này"));

        if (source.getSoluong() < req.quantity()) {
            throw new RuntimeException("Cơ sở xuất chỉ còn " + source.getSoluong() + " sản phẩm, không đủ để chuyển " + req.quantity());
        }

        Branch targetBranch = branchRepository.findById(req.toBranchId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ sở nhận"));

        source.setSoluong(source.getSoluong() - req.quantity());
        inventoryRepository.save(source);

        Inventory target = inventoryRepository.findByProducts_IdAndBranch_Id(req.productId(), req.toBranchId())
                .orElseGet(() -> {
                    Inventory created = new Inventory();
                    created.setProducts(source.getProducts());
                    created.setBranch(targetBranch);
                    created.setSoluong(0);
                    created.setSelled(0);
                    return created;
                });
        target.setSoluong(target.getSoluong() + req.quantity());
        inventoryRepository.save(target);

        broadcastProductTotalStock(req.productId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> showInventory(Long branchId, String city, String keyword, String stockState, Pageable pageable) {
        Page<Inventory> page = inventoryRepository.searchMultiBranchInventory(
                branchId,
                city != null ? city.trim() : null,
                keyword != null ? keyword.trim() : null,
                stockState != null ? stockState.trim() : null,
                pageable
        );
        return page.map(inventoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse searchInventory(String nameProduct) {
        List<Inventory> list = inventoryRepository.searchMultiBranchInventory(
                null, null, nameProduct, null, Pageable.ofSize(1)
        ).getContent();
        if (list.isEmpty()) {
            throw new RuntimeException("Không tìm thấy tồn kho cho sản phẩm: " + nameProduct);
        }
        return inventoryMapper.toResponse(list.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponseUser detailInventory(Long productId, Long branchId, String city) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        List<Branch> activeBranches = branchRepository.findByActiveTrueOrderByIdAsc();
        List<Inventory> records = inventoryRepository.findAllByProductIdWithBranch(productId);
        Map<Long, Inventory> recordByBranch = records.stream()
                .filter(r -> r.getBranch() != null)
                .collect(Collectors.toMap(r -> r.getBranch().getId(), r -> r, (a, b) -> a));

        List<InventoryResponse> branchResponses = new ArrayList<>();
        int totalStock = 0;
        int locationStock = 0;
        int availableBranchCount = 0;

        boolean filterByBranch = branchId != null;
        boolean filterByCity = !filterByBranch && city != null && !city.isBlank();

        for (Branch b : activeBranches) {
            Inventory inv = recordByBranch.get(b.getId());
            int qty = inv != null ? Math.max(0, inv.getSoluong()) : 0;
            int selled = inv != null ? Math.max(0, inv.getSelled()) : 0;
            Long invId = inv != null ? inv.getId() : 0L;

            totalStock += qty;
            if (qty > 0) {
                availableBranchCount++;
            }

            if (filterByBranch && b.getId().equals(branchId)) {
                locationStock += qty;
            } else if (filterByCity && b.getCity() != null && b.getCity().equalsIgnoreCase(city.trim())) {
                locationStock += qty;
            }

            branchResponses.add(new InventoryResponse(
                    invId,
                    product.getId(),
                    product.getNameProduct(),
                    b.getId(),
                    b.getCode(),
                    b.getName(),
                    b.getCity(),
                    b.getAddress(),
                    b.getPhone(),
                    qty,
                    selled
            ));
        }

        int effectiveStock = (filterByBranch || filterByCity) ? locationStock : totalStock;
        Long representativeInvId = records.isEmpty() ? productId : records.get(0).getId();

        return new InventoryResponseUser(
                representativeInvId,
                product.getId(),
                product.getNameProduct(),
                effectiveStock,
                totalStock,
                availableBranchCount,
                branchResponses
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> listBranchStocksByProduct(Long productId) {
        return detailInventory(productId, null, null).branches();
    }

    private void broadcastProductTotalStock(Long productId) {
        List<Inventory> all = inventoryRepository.findByProducts_Id(productId);
        int total = all.stream().mapToInt(i -> Math.max(0, i.getSoluong())).sum();
        Long invId = all.isEmpty() ? productId : all.get(0).getId();
        inventoryWebSocketService.sendStockUpdate(invId, productId, total);
    }
}
