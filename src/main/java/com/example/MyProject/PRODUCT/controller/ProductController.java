package com.example.MyProject.PRODUCT.controller;

import com.example.MyProject.common.ApiResponse;
import com.example.MyProject.common.BaseController;
import com.example.MyProject.common.ProcessPageable;
import com.example.MyProject.PRODUCT.dto.product.request.RequestProduct;
import com.example.MyProject.PRODUCT.dto.product.response.ResponseProduct;
import com.example.MyProject.PRODUCT.dto.product.response.ResponseSearchProduct;
import com.example.MyProject.PRODUCT.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController extends BaseController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<Page<ResponseProduct>> showAll(
            @RequestParam(name = "page_no", defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = ProcessPageable.createPageable(page, size, sortBy, sortDir);
        return successDataResponse("Get Product success", productService.ListAll(pageable));
    }

    @GetMapping("/catalog")
    public ApiResponse<Page<ResponseProduct>> showCatalog(
            @RequestParam(name = "group_type", required = false) String groupType,
            @RequestParam(name = "category_id", required = false) Long categoryId,
            @RequestParam(name = "brand_id", required = false) Long brandId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "min_price", required = false) BigDecimal minPrice,
            @RequestParam(name = "max_price", required = false) BigDecimal maxPrice,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "branch_id", required = false) Long branchId,
            @RequestParam(name = "in_stock_only", required = false) Boolean inStockOnly,
            @RequestParam(name = "sort_mode", defaultValue = "newest") String sortMode,
            @RequestParam(name = "page_no", defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int size
    ) {
        return successDataResponse(
                "Get catalog success",
                productService.filterCatalog(
                        groupType, categoryId, brandId, keyword, minPrice, maxPrice,
                        city, branchId, inStockOnly, sortMode, page, size
                )
        );
    }

    @GetMapping("/{id}/product_brands")
    public ApiResponse<Page<ResponseProduct>> showProductwithBrands(
            @PathVariable("id") Long id,
            @RequestParam(name = "page_no", defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = ProcessPageable.createPageable(page, size, sortBy, sortDir);
        return successDataResponse("Get Product success", productService.listWithBrands(id, pageable));
    }

    @GetMapping("/{id}/product_categories")
    public ApiResponse<Page<ResponseProduct>> showProductwithCategories(
            @PathVariable("id") Long id,
            @RequestParam(name = "page_no", defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = ProcessPageable.createPageable(page, size, sortBy, sortDir);
        return successDataResponse("Get Product success", productService.listWithCategories(id, pageable));
    }

    @GetMapping("/{id1}/{id2}/product_categories_brands")
    public ApiResponse<Page<ResponseProduct>> showProductwithCategoriesandBrands(
            @PathVariable("id1") Long id1,
            @PathVariable("id2") Long id2,
            @RequestParam(name = "page_no", defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = ProcessPageable.createPageable(page, size, sortBy, sortDir);
        return successDataResponse("Get Product success", productService.listWithCategoriesAndBrands(id1, id2, pageable));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<String> createProduct(@RequestBody RequestProduct requestProduct) {
        productService.createProduct(requestProduct);
        return successResponse("Create product success!");
    }

    @PutMapping("/{id}/update")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<String> updateProduct(@PathVariable("id") Long id, @RequestBody RequestProduct requestProduct) {
        productService.updateProduct(id, requestProduct);
        return successResponse("Update product success!");
    }

    @DeleteMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<String> deleteProduct(@PathVariable("id") Long id) {
        productService.deleteProduct(id);
        return successResponse("delete product success!");
    }

    @GetMapping("/search")
    public ApiResponse<ResponseSearchProduct> searchProduct(
            @RequestParam("nameProduct") String nameProduct,
            @RequestParam(name = "page_no", defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Pageable pageable = ProcessPageable.createPageable(page, size, sortBy, sortDir);
        ResponseSearchProduct responseSearchProduct = productService.searchProduct(nameProduct, pageable);
        return successDataResponse("Search success", responseSearchProduct);
    }

    @GetMapping("/{id}/detailproduct")
    public ApiResponse<ResponseProduct> Showonly(@PathVariable("id") Long id) {
        return successDataResponse("Đã tìm thấy", productService.Showonly(id));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponse<ResponseProduct> getBySlug(@PathVariable("slug") String slug) {
        return successDataResponse("Đã tìm thấy", productService.getBySlug(slug));
    }
}
