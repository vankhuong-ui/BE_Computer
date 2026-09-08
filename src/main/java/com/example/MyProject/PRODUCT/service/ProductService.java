package com.example.MyProject.PRODUCT.service;

import com.example.MyProject.PRODUCT.dto.product.request.RequestProduct;
import com.example.MyProject.PRODUCT.dto.product.response.ResponseProduct;
import com.example.MyProject.PRODUCT.dto.product.response.ResponseSearchProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    void createProduct(RequestProduct requestProduct);
    void updateProduct(Long id,RequestProduct requestProduct);
    Page<ResponseProduct> listWithCategories(Long id,Pageable pageable);
    Page<ResponseProduct> listWithBrands(Long id,Pageable pageable);
    Page<ResponseProduct> listWithCategoriesAndBrands(Long id1,Long id2,Pageable pageable);
    Page<ResponseProduct> ListAll(Pageable pageable);
    void deleteProduct(Long id);
    ResponseSearchProduct searchProduct(String nameProduct,Pageable pageable);
    ResponseProduct Showonly(Long id);
}
