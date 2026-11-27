package com.example.MyProject.PRODUCT.service;

import com.example.MyProject.PRODUCT.dto.product.request.RequestProduct;
import com.example.MyProject.PRODUCT.dto.product.response.ResponseProduct;
import com.example.MyProject.PRODUCT.dto.product.response.ResponseSearchProduct;
import com.example.MyProject.PRODUCT.entity.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    void createProduct(RequestProduct requestProduct);
    void updateProduct(Long id, RequestProduct requestProduct);
    Page<ResponseProduct> listWithCategories(Long id, Pageable pageable);
    Page<ResponseProduct> listWithBrands(Long id, Pageable pageable);
    Page<ResponseProduct> listWithCategoriesAndBrands(Long categoryId, Long brandId, Pageable pageable);
    Page<ResponseProduct> ListAll(Pageable pageable);
    Page<ResponseProduct> filterCatalog(
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
    );
    void deleteProduct(Long id);
    ResponseSearchProduct searchProduct(String nameProduct, Pageable pageable);
    ResponseProduct Showonly(Long id);
    ResponseProduct getBySlug(String slug);

    Products requireProductById(Long productId);
    ResponseProduct toProductResponse(Products product);
    List<Products> findAllCatalogProductsWithBrandAndCategory();
    long countTotalProducts();
}
