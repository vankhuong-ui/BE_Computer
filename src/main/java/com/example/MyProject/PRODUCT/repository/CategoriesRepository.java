package com.example.MyProject.PRODUCT.repository;

import com.example.MyProject.PRODUCT.entity.Categories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriesRepository extends JpaRepository<Categories, Long> {
    Optional<Categories> findBySlugIgnoreCase(String slug);
    Optional<Categories> findByNameCategoriesIgnoreCase(String nameCategories);
}
