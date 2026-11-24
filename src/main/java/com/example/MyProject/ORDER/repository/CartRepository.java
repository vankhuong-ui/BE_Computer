package com.example.MyProject.ORDER.repository;

import com.example.MyProject.ORDER.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Cart findByAccountIdAndProductsId(Long accountId, Long productId);

    @Query("""
        SELECT DISTINCT c
        FROM Cart c
        JOIN FETCH c.products p
        JOIN FETCH p.brands
        JOIN FETCH p.categories
        WHERE c.account.id = :accountId
    """)
    List<Cart> findByAccountId(@Param("accountId") Long id);

    @Modifying
    @Query("""
        DELETE FROM Cart c
        WHERE c.account.id = :accountId
          AND c.products.id IN :productIds
    """)
    void deleteByAccountIdAndProductIds(
            @Param("accountId") Long accountId,
            @Param("productIds") List<Long> productIds
    );
}
