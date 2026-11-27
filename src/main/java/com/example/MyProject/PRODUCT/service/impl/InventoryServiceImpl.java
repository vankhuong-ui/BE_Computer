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
import com.example.MyProject.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional
    public void createInventory(InventoryRequest req) {
        if (req.productId() == null) {
            throw new BusinessException("Vui lòng chọn sản phẩm");
        }
        if (req.soluong() < 0) {
            throw new BusinessException("Số lượng tồn kho không được âm");
        }

        Products product = productRepository.findById(req.productId())
                .orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại"));

        if (Boolean.TRUE.equals(req.allBranches())) {
            List<Branch> branches = branchRepository.findByActiveTrueOrderByIdAsc();
            if (branches.isEmpty()) {
                throw new BusinessException("Chưa có cơ sở nào đang hoạt động");
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
            return;
        }

        Branch branch;
        if (req.branchId() != null) {
            branch = branchRepository.findById(req.branchId())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy cơ sở #" + req.branchId()));
        } else {
            branch = branchRepository.findAllByOrderByIdAsc().stream().findFirst()
                    .orElseThrow(() -> new BusinessException("Hệ thống chưa có cơ sở nào"));
        }

        Optional<Inventory> existing = inventoryRepository.findByProducts_IdAndBranch_Id(product.getId(), branch.getId());
        if (existing.isPresent()) {
            Inventory inv = existing.get();
            inv.setSoluong(inv.getSoluong() + req.soluong());
            inventoryRepository.save(inv);
            return;
        }

        Inventory inv = new Inventory();
        inv.setProducts(product);
        inv.setBranch(branch);
        inv.setSoluong(req.soluong());
        inv.setSelled(0);
        inventoryRepository.save(inv);
    }

    @Override
    @Transactional
    public void updateSoluong(Long id, int luongnhap) {
        if (luongnhap <= 0) {
            throw new BusinessException("Số lượng nhập thêm phải lớn hơn 0");
        }
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Bản ghi tồn kho không tồn tại"));
        inventory.setSoluong(Math.max(0, inventory.getSoluong()) + luongnhap);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void adjustSoluong(Long id, int exactSoluong) {
        if (exactSoluong < 0) {
            throw new BusinessException("Số lượng kiểm kê thực tế không được âm");
        }
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Bản ghi tồn kho không tồn tại"));
        inventory.setSoluong(exactSoluong);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void transferStock(InventoryTransferRequest req) {
        if (req.productId() == null || req.fromBranchId() == null || req.toBranchId() == null) {
            throw new BusinessException("Vui lòng chọn đầy đủ sản phẩm, cơ sở xuất và cơ sở nhận");
        }
        if (req.fromBranchId().equals(req.toBranchId())) {
            throw new BusinessException("Cơ sở xuất và cơ sở nhận phải khác nhau");
        }
        if (req.quantity() <= 0) {
            throw new BusinessException("Số lượng chuyển kho phải lớn hơn 0");
        }

        Inventory source = inventoryRepository.findByProducts_IdAndBranch_Id(req.productId(), req.fromBranchId())
                .orElseThrow(() -> new BusinessException("Cơ sở xuất chưa có kho cho sản phẩm này"));

        if (source.getSoluong() < req.quantity()) {
            throw new BusinessException("Cơ sở xuất chỉ còn " + source.getSoluong() + " sản phẩm, không đủ để chuyển " + req.quantity());
        }

        Branch targetBranch = branchRepository.findById(req.toBranchId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy cơ sở nhận"));

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
            throw new BusinessException("Không tìm thấy tồn kho cho sản phẩm: " + nameProduct);
        }
        return inventoryMapper.toResponse(list.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponseUser detailInventory(Long productId, Long branchId, String city) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại"));

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

    @Override
    @Transactional
    public Branch allocateStockForOrder(
            Products product,
            int quantity,
            boolean isStorePickup,
            Branch pickupBranch,
            Long preferredBranchId,
            String preferredCity,
            String shippingAddressText
    ) {
        if (isStorePickup) {
            if (pickupBranch == null) {
                throw new BusinessException("Vui lòng chọn cơ sở cửa hàng để đến nhận máy");
            }
            int updatedRows = inventoryRepository.decreaseStockAtBranch(product.getId(), pickupBranch.getId(), quantity);
            if (updatedRows == 0) {
                throw new BusinessException(
                        "Sản phẩm \"" + product.getNameProduct() + "\" không đủ số lượng tại cơ sở " + pickupBranch.getName()
                );
            }
            return pickupBranch;
        }

        List<Inventory> available = inventoryRepository.findAllByProductIdWithBranch(product.getId()).stream()
                .filter(i -> i.getBranch() != null && i.getBranch().isActive() && i.getSoluong() >= quantity)
                .toList();

        if (available.isEmpty()) {
            throw new BusinessException("Sản phẩm \"" + product.getNameProduct() + "\" hiện không đủ số lượng (" + quantity + ") tại các cơ sở.");
        }

        Inventory chosen = null;
        if (preferredBranchId != null) {
            chosen = available.stream()
                    .filter(i -> preferredBranchId.equals(i.getBranch().getId()))
                    .findFirst()
                    .orElse(null);
        }

        if (chosen == null) {
            String targetCity = (preferredCity != null && !preferredCity.isBlank()) ? preferredCity.trim().toLowerCase() : null;
            String addrLower = shippingAddressText != null ? shippingAddressText.toLowerCase() : "";
            chosen = available.stream()
                    .filter(i -> {
                        String bCity = i.getBranch().getCity() != null ? i.getBranch().getCity().toLowerCase() : "";
                        return (targetCity != null && bCity.equals(targetCity))
                                || (!bCity.isEmpty() && addrLower.contains(bCity));
                    })
                    .max(Comparator.comparingInt(Inventory::getSoluong))
                    .orElse(null);
        }

        if (chosen == null) {
            chosen = available.stream()
                    .max(Comparator.comparingInt(Inventory::getSoluong))
                    .orElse(available.get(0));
        }

        int updatedRows = inventoryRepository.decreaseStockAtBranch(product.getId(), chosen.getBranch().getId(), quantity);
        if (updatedRows == 0) {
            throw new BusinessException("Sản phẩm \"" + product.getNameProduct() + "\" vừa hết hàng, vui lòng thử lại.");
        }
        return chosen.getBranch();
    }

    @Override
    @Transactional
    public void restoreStock(Long productId, Long branchId, int quantity) {
        if (productId == null || quantity <= 0) {
            return;
        }
        if (branchId != null) {
            int updatedRows = inventoryRepository.increaseStockAtBranch(productId, branchId, quantity);
            if (updatedRows > 0) {
                return;
            }
        }
        List<Inventory> invs = inventoryRepository.findByProducts_Id(productId);
        if (!invs.isEmpty()) {
            Inventory first = invs.get(0);
            first.setSoluong(first.getSoluong() + quantity);
            inventoryRepository.save(first);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> findInventoriesByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return inventoryRepository.findByProducts_IdIn(productIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> findAllInventories() {
        return inventoryRepository.findAll();
    }
}
