package com.example.MyProject.PRODUCT.repository;

import com.example.MyProject.PRODUCT.entity.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Products, Long> {
    Page<Products> findByCategories_Id(Long id, Pageable pageable);

    Page<Products> findByBrands_Id(Long id, Pageable pageable);

    Page<Products> findByBrands_IdAndCategories_Id(Long brandId, Long categoryId, Pageable pageable);

    Optional<Products> findByNameProduct(String name);

    Optional<Products> findBySlug(String slug);

    @Query("SELECT p FROM Products p LEFT JOIN FETCH p.brands LEFT JOIN FETCH p.categories WHERE LOWER(p.slug) = LOWER(:slug)")
    Optional<Products> findBySlugWithBrandAndCategory(@Param("slug") String slug);

    List<Products> findByNameProductContainingIgnoreCase(String nameProduct);

    @Query("SELECT p FROM Products p LEFT JOIN FETCH p.brands LEFT JOIN FETCH p.categories " +
            "WHERE LOWER(p.nameProduct) LIKE LOWER(CONCAT('%', :kw, '%')) " +
            "OR LOWER(COALESCE(p.specs, '')) LIKE LOWER(CONCAT('%', :kw, '%')) " +
            "OR LOWER(COALESCE(p.brands.nameBrands, '')) LIKE LOWER(CONCAT('%', :kw, '%')) " +
            "OR LOWER(COALESCE(p.categories.nameCategories, '')) LIKE LOWER(CONCAT('%', :kw, '%'))")
    Page<Products> searchByKeyword(@Param("kw") String keyword, Pageable pageable);

    @Query("SELECT p FROM Products p LEFT JOIN FETCH p.brands LEFT JOIN FETCH p.categories ORDER BY p.id DESC")
    List<Products> findAllWithBrandAndCategory();
}
