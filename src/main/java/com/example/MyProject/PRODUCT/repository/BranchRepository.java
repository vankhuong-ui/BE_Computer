package com.example.MyProject.PRODUCT.repository;

import com.example.MyProject.PRODUCT.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByActiveTrueOrderByIdAsc();
    List<Branch> findByActiveTrueAndCityIgnoreCaseOrderByIdAsc(String city);
    List<Branch> findAllByOrderByIdAsc();
    Optional<Branch> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
}
