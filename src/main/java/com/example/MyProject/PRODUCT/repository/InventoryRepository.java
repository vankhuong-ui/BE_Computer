package com.example.MyProject.PRODUCT.repository;

import com.example.MyProject.PRODUCT.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Query("SELECT i FROM Inventory i JOIN FETCH i.products JOIN FETCH i.branch WHERE i.products.id = :productId ORDER BY i.branch.id ASC")
    List<Inventory> findAllByProductIdWithBranch(@Param("productId") Long productId);

    Optional<Inventory> findByProducts_IdAndBranch_Id(Long productId, Long branchId);

    List<Inventory> findByProducts_Id(Long productId);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.products JOIN FETCH i.branch WHERE i.products.id IN :productIds")
    List<Inventory> findByProducts_IdIn(@Param("productIds") List<Long> productIds);

    List<Inventory> findByBranch_Id(Long branchId);

    List<Inventory> findByBranch_IdIn(List<Long> branchIds);

    @Query(
            value = "SELECT i FROM Inventory i JOIN FETCH i.products p JOIN FETCH i.branch b " +
                    "WHERE (:branchId IS NULL OR b.id = :branchId) " +
                    "AND (:city IS NULL OR :city = '' OR LOWER(b.city) = LOWER(:city)) " +
                    "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.nameProduct) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                    "AND (:stockState IS NULL OR :stockState = '' " +
                    "     OR (:stockState = 'IN_STOCK' AND i.soluong > 0) " +
                    "     OR (:stockState = 'LOW_STOCK' AND i.soluong > 0 AND i.soluong <= 3) " +
                    "     OR (:stockState = 'OUT_OF_STOCK' AND i.soluong <= 0))",
            countQuery = "SELECT COUNT(i) FROM Inventory i JOIN i.products p JOIN i.branch b " +
                    "WHERE (:branchId IS NULL OR b.id = :branchId) " +
                    "AND (:city IS NULL OR :city = '' OR LOWER(b.city) = LOWER(:city)) " +
                    "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.nameProduct) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                    "AND (:stockState IS NULL OR :stockState = '' " +
                    "     OR (:stockState = 'IN_STOCK' AND i.soluong > 0) " +
                    "     OR (:stockState = 'LOW_STOCK' AND i.soluong > 0 AND i.soluong <= 3) " +
                    "     OR (:stockState = 'OUT_OF_STOCK' AND i.soluong <= 0))"
    )
    Page<Inventory> searchMultiBranchInventory(
            @Param("branchId") Long branchId,
            @Param("city") String city,
            @Param("keyword") String keyword,
            @Param("stockState") String stockState,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Inventory i SET i.soluong = i.soluong - :quantity, i.selled = i.selled + :quantity " +
            "WHERE i.products.id = :productId AND i.branch.id = :branchId AND i.soluong >= :quantity")
    int decreaseStockAtBranch(@Param("productId") Long productId, @Param("branchId") Long branchId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Inventory i SET i.soluong = i.soluong + :quantity, i.selled = CASE WHEN i.selled >= :quantity THEN i.selled - :quantity ELSE 0 END " +
            "WHERE i.products.id = :productId AND i.branch.id = :branchId")
    int increaseStockAtBranch(@Param("productId") Long productId, @Param("branchId") Long branchId, @Param("quantity") int quantity);
}
