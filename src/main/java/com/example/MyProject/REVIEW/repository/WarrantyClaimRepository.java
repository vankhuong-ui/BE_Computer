package com.example.MyProject.REVIEW.repository;

import com.example.MyProject.REVIEW.entity.WarrantyClaim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaim, Long> {

    @EntityGraph(attributePaths = {"order", "product", "account"})
    Page<WarrantyClaim> findByAccountId(Long accountId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"order", "product", "account"})
    Page<WarrantyClaim> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"order", "product", "account"})
    List<WarrantyClaim> findBySerialCodeIgnoreCaseOrOrderId(String serialCode, Long orderId);

    @EntityGraph(attributePaths = {"order", "product", "account"})
    List<WarrantyClaim> findBySerialCodeContainingIgnoreCase(String serialCode);

    Optional<WarrantyClaim> findFirstByOrderIdAndProductIdAndUnitNumberAndStatusIn(
            Long orderId,
            Long productId,
            Integer unitNumber,
            List<String> statuses
    );
}
