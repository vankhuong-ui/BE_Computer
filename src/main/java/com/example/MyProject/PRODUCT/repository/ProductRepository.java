package com.example.MyProject.PRODUCT.repository;

import com.example.MyProject.PRODUCT.entity.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Products, Long> {
    // Tìm sản phẩm theo Category
    Page<Products> findByCategories_Id(Long id, Pageable pageable);

    // Tìm sản phẩm theo Brand
    Page<Products> findByBrands_Id(Long id, Pageable pageable);

    // Tìm sản phẩm theo Brand + Category
    Page<Products> findByBrands_IdAndCategories_Id(Long brandId, Long categoryId, Pageable pageable);
    Optional<Products> findByNameProduct(String name);
    List<Products> findByNameProductContainingIgnoreCase(String nameProduct);

    @Query("SELECT p FROM Products p LEFT JOIN FETCH p.brands LEFT JOIN FETCH p.categories ORDER BY p.id DESC")
    List<Products> findAllWithBrandAndCategory();
}
