package com.example.MyProject.PRODUCT.service.impl;

import com.example.MyProject.PRODUCT.dto.inventory.InventoryRequest;
import com.example.MyProject.PRODUCT.dto.product.request.RequestProduct;
import com.example.MyProject.PRODUCT.dto.product.response.ResponseProduct;
import com.example.MyProject.PRODUCT.dto.product.response.ResponseSearchProduct;
import com.example.MyProject.PRODUCT.entity.Brands;
import com.example.MyProject.PRODUCT.entity.Categories;
import com.example.MyProject.PRODUCT.entity.Inventory;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.mapper.ProductMapper;
import com.example.MyProject.PRODUCT.repository.BrandsRepository;
import com.example.MyProject.PRODUCT.repository.CategoriesRepository;
import com.example.MyProject.PRODUCT.repository.InventoryRepository;
import com.example.MyProject.PRODUCT.repository.ProductRepository;
import com.example.MyProject.PRODUCT.service.InventoryService;
import com.example.MyProject.PRODUCT.service.ProductService;
import com.example.MyProject.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BrandsRepository brandsRepository;
    private final CategoriesRepository categoriesRepository;
    private final ProductMapper productMapper;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public void createProduct(RequestProduct requestProduct) {
        Products entity = productMapper.toEntity(requestProduct);
        if (entity.getSlug() == null || entity.getSlug().isBlank()) {
            entity.setSlug(generateUniqueSlug(requestProduct.nameProduct()));
        }
        if (requestProduct.brandsId() != null) {
            Brands b = brandsRepository.findById(requestProduct.brandsId())
                    .orElseThrow(() -> new BusinessException("Thương hiệu không tồn tại"));
            entity.setBrands(b);
        }
        if (requestProduct.categoriesId() != null) {
            Categories c = categoriesRepository.findById(requestProduct.categoriesId())
                    .orElseThrow(() -> new BusinessException("Danh mục không tồn tại"));
            entity.setCategories(c);
        }
        Products saved = productRepository.save(entity);

        if (requestProduct.initialStock() != null && requestProduct.initialStock() >= 0) {
            inventoryService.createInventory(new InventoryRequest(
                    saved.getId(),
                    requestProduct.initialBranchId(),
                    requestProduct.initialStock(),
                    requestProduct.allBranches()
            ));
        }
    }

    @Override
    @Transactional
    public void updateProduct(Long id, RequestProduct requestProduct) {
        Products products = requireProductById(id);
        productMapper.updateProduct(requestProduct, products);
        if (requestProduct.brandsId() != null) {
            Brands b = brandsRepository.findById(requestProduct.brandsId())
                    .orElseThrow(() -> new BusinessException("Thương hiệu không tồn tại"));
            products.setBrands(b);
        }
        if (requestProduct.categoriesId() != null) {
            Categories c = categoriesRepository.findById(requestProduct.categoriesId())
                    .orElseThrow(() -> new BusinessException("Danh mục không tồn tại"));
            products.setCategories(c);
        }
        if (products.getSlug() == null || products.getSlug().isBlank()) {
            products.setSlug(generateUniqueSlug(products.getNameProduct()));
        }
        productRepository.save(products);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseProduct> listWithCategories(Long id, Pageable pageable) {
        Page<Products> pageproduct = productRepository.findByCategories_Id(id, pageable);
        return enrichProductPage(pageproduct, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseProduct> listWithBrands(Long id, Pageable pageable) {
        Page<Products> pageproduct = productRepository.findByBrands_Id(id, pageable);
        return enrichProductPage(pageproduct, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseProduct> listWithCategoriesAndBrands(Long categoryId, Long brandId, Pageable pageable) {
        Page<Products> pageproduct = productRepository.findByBrands_IdAndCategories_Id(brandId, categoryId, pageable);
        return enrichProductPage(pageproduct, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseProduct> ListAll(Pageable pageable) {
        Page<Products> pageproduct = productRepository.findAll(pageable);
        return enrichProductPage(pageproduct, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResponseProduct> filterCatalog(
            String groupType,
            Long categoryId,
            Long brandId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String city,
            Long branchId,
            Boolean inStockOnly,
            String sortMode,
            int page,
            int size
    ) {
        List<Products> all = productRepository.findAllWithBrandAndCategory();
        String normGroup = (groupType != null && !groupType.isBlank()) ? groupType.trim().toUpperCase() : null;
        String normKw = (keyword != null && !keyword.isBlank()) ? keyword.trim().toLowerCase() : null;

        List<Products> matched = all.stream().filter(p -> {
            if (categoryId != null) {
                if (p.getCategories() == null || !categoryId.equals(p.getCategories().getId())) return false;
            }
            if (brandId != null) {
                if (p.getBrands() == null || !brandId.equals(p.getBrands().getId())) return false;
            }
            if (normGroup != null && !"ALL".equals(normGroup)) {
                String pGroup = (p.getCategories() != null && p.getCategories().getGroupType() != null)
                        ? p.getCategories().getGroupType().toUpperCase()
                        : "LAPTOP";
                if (!normGroup.equals(pGroup)) return false;
            }
            if (minPrice != null && (p.getPrice() == null || p.getPrice().compareTo(minPrice) < 0)) {
                return false;
            }
            if (maxPrice != null && (p.getPrice() == null || p.getPrice().compareTo(maxPrice) > 0)) {
                return false;
            }
            if (normKw != null) {
                String hay = ((p.getNameProduct() != null ? p.getNameProduct() : "") + " " +
                        (p.getSpecs() != null ? p.getSpecs() : "") + " " +
                        (p.getBrands() != null && p.getBrands().getNameBrands() != null ? p.getBrands().getNameBrands() : "") + " " +
                        (p.getCategories() != null && p.getCategories().getNameCategories() != null ? p.getCategories().getNameCategories() : "")
                ).toLowerCase();
                if (!hay.contains(normKw)) return false;
            }
            return true;
        }).toList();

        List<ResponseProduct> enriched = enrichProductList(matched, branchId, city);

        if (Boolean.TRUE.equals(inStockOnly)) {
            enriched = enriched.stream()
                    .filter(rp -> (rp.locationStock() != null ? rp.locationStock() : 0) > 0)
                    .toList();
        }

        List<ResponseProduct> sorted = new ArrayList<>(enriched);
        String sm = sortMode != null ? sortMode.trim().toLowerCase() : "newest";
        switch (sm) {
            case "price_asc" -> sorted.sort(Comparator.comparing(ResponseProduct::price, Comparator.nullsLast(Comparator.naturalOrder())));
            case "price_desc" -> sorted.sort(Comparator.comparing(ResponseProduct::price, Comparator.nullsLast(Comparator.reverseOrder())));
            case "featured", "bestselling" -> sorted.sort(
                    Comparator.comparing((ResponseProduct r) -> r.locationStock() != null && r.locationStock() > 0 ? 1 : 0).reversed()
                            .thenComparing(r -> r.totalStock() != null ? r.totalStock() : 0, Comparator.reverseOrder())
                            .thenComparing(ResponseProduct::id, Comparator.nullsLast(Comparator.reverseOrder()))
            );
            default -> sorted.sort(Comparator.comparing(ResponseProduct::id, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        int start = safePage * safeSize;
        int end = Math.min(start + safeSize, sorted.size());
        List<ResponseProduct> pageContent = start < sorted.size() ? sorted.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, PageRequest.of(safePage, safeSize), sorted.size());
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        List<Inventory> inventories = inventoryRepository.findByProducts_Id(id);
        int totalStock = inventories.stream().mapToInt(i -> Math.max(0, i.getSoluong())).sum();
        if (totalStock > 0) {
            throw new BusinessException("Sản phẩm vẫn còn " + totalStock + " tồn kho tại các cơ sở, không thể xóa!");
        }
        inventoryRepository.deleteAll(inventories);
        productRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseSearchProduct searchProduct(String nameProduct, Pageable pageable) {
        String kw = nameProduct != null ? nameProduct.trim() : "";
        Page<Products> pageproduct = productRepository.searchByKeyword(kw, pageable);
        Page<ResponseProduct> enrichedPage = enrichProductPage(pageproduct, null, null);
        ResponseProduct first = enrichedPage.hasContent() ? enrichedPage.getContent().get(0) : null;
        return new ResponseSearchProduct(first, enrichedPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseProduct Showonly(Long id) {
        Products p = requireProductById(id);
        return enrichProductList(List.of(p), null, null).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseProduct getBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new BusinessException("Thiếu đường dẫn sản phẩm (slug)");
        }
        Products p = productRepository.findBySlugWithBrandAndCategory(slug.trim())
                .orElseThrow(() -> new BusinessException("Không tìm thấy sản phẩm: " + slug));
        return enrichProductList(List.of(p), null, null).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Products requireProductById(Long productId) {
        if (productId == null) {
            throw new BusinessException("Mã sản phẩm không được để trống");
        }
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Sản phẩm không tồn tại: #" + productId));
    }

    @Override
    public ResponseProduct toProductResponse(Products product) {
        return productMapper.toReponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Products> findAllCatalogProductsWithBrandAndCategory() {
        return productRepository.findAllWithBrandAndCategory();
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalProducts() {
        return productRepository.count();
    }

    private Page<ResponseProduct> enrichProductPage(Page<Products> page, Long branchId, String city) {
        List<ResponseProduct> list = enrichProductList(page.getContent(), branchId, city);
        return new PageImpl<>(list, page.getPageable(), page.getTotalElements());
    }

    private List<ResponseProduct> enrichProductList(List<Products> products, Long branchId, String city) {
        if (products.isEmpty()) return List.of();
        List<Long> pIds = products.stream().map(Products::getId).filter(Objects::nonNull).toList();
        List<Inventory> allInv = pIds.isEmpty() ? List.of() : inventoryRepository.findByProducts_IdIn(pIds);

        Map<Long, List<Inventory>> invByProduct = new HashMap<>();
        for (Inventory inv : allInv) {
            if (inv.getProducts() == null || inv.getProducts().getId() == null) continue;
            if (inv.getBranch() != null && !inv.getBranch().isActive()) continue;
            invByProduct.computeIfAbsent(inv.getProducts().getId(), k -> new ArrayList<>()).add(inv);
        }

        boolean filterByBranch = branchId != null;
        boolean filterByCity = !filterByBranch && city != null && !city.isBlank();

        List<ResponseProduct> result = new ArrayList<>(products.size());
        for (Products p : products) {
            ResponseProduct base = productMapper.toReponse(p);
            List<Inventory> pInvs = invByProduct.getOrDefault(p.getId(), List.of());

            int totalStock = 0;
            int locStock = 0;
            int availBranches = 0;

            for (Inventory inv : pInvs) {
                int q = Math.max(0, inv.getSoluong());
                totalStock += q;
                if (q > 0) availBranches++;
                if (filterByBranch && inv.getBranch() != null && branchId.equals(inv.getBranch().getId())) {
                    locStock += q;
                } else if (filterByCity && inv.getBranch() != null && inv.getBranch().getCity() != null
                        && inv.getBranch().getCity().equalsIgnoreCase(city.trim())) {
                    locStock += q;
                }
            }

            int effectiveLocStock = (filterByBranch || filterByCity) ? locStock : totalStock;
            String groupType = base.groupType() != null && !base.groupType().isBlank() ? base.groupType() : "LAPTOP";

            result.add(new ResponseProduct(
                    base.id(),
                    base.nameProduct(),
                    base.slug(),
                    base.price(),
                    base.specs(),
                    base.image(),
                    base.description(),
                    base.brandsId(),
                    base.categoriesId(),
                    base.brandsName(),
                    base.categoriesName(),
                    groupType,
                    totalStock,
                    effectiveLocStock,
                    availBranches
            ));
        }
        return result;
    }

    private String generateUniqueSlug(String name) {
        String base = name == null ? "san-pham" : Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "san-pham";
        String candidate = base;
        int suffix = 1;
        while (productRepository.findBySlug(candidate).isPresent()) {
            candidate = base + "-" + (suffix++);
        }
        return candidate;
    }
}
