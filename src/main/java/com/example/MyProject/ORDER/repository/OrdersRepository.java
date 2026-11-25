package com.example.MyProject.ORDER.repository;

import com.example.MyProject.ORDER.entity.Orders;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    List<Orders> findByUseraddressesAccountId(Long accountId);

    @Query("""
        SELECT DISTINCT o
        FROM Orders o
        JOIN FETCH o.useraddresses ua
        LEFT JOIN FETCH o.pickupBranch
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.branch
        LEFT JOIN FETCH oi.products p
        LEFT JOIN FETCH p.brands
        LEFT JOIN FETCH p.categories
        WHERE ua.account.id = :accountId
        ORDER BY o.id DESC
    """)
    List<Orders> findOrdersWithDetailsByAccountId(
            @Param("accountId") Long accountId
    );

    @Modifying
    @Query("""
        UPDATE Orders o
        SET o.totalAmount = :totalAmount
        WHERE o.id = :orderId
    """)
    int updateTotalPrice(
            @Param("orderId") Long orderId,
            @Param("totalAmount") BigDecimal totalAmount
    );

    Optional<Orders> findByIdAndUseraddressesAccountId(
            Long orderId,
            Long accountId
    );

    @Query("""
        SELECT DISTINCT o
        FROM Orders o
        JOIN FETCH o.useraddresses ua
        LEFT JOIN FETCH o.pickupBranch
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.branch
        LEFT JOIN FETCH oi.products p
        LEFT JOIN FETCH p.brands
        LEFT JOIN FETCH p.categories
        WHERE o.id = :orderId
          AND ua.account.id = :accountId
    """)
    Optional<Orders> findByIdAndAccountIdWithDetails(
            @Param("orderId") Long orderId,
            @Param("accountId") Long accountId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o
        FROM Orders o
        JOIN o.useraddresses ua
        WHERE o.id = :orderId
          AND ua.account.id = :accountId
    """)
    Optional<Orders> findForWarrantyClaimByIdAndAccountId(
            @Param("orderId") Long orderId,
            @Param("accountId") Long accountId
    );

    @Query("""
        SELECT DISTINCT o
        FROM Orders o
        LEFT JOIN FETCH o.useraddresses ua
        LEFT JOIN FETCH o.pickupBranch
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.branch
        LEFT JOIN FETCH oi.products p
        LEFT JOIN FETCH p.brands
        LEFT JOIN FETCH p.categories
        WHERE o.id = :orderId
    """)
    Optional<Orders> findByIdWithDetails(
            @Param("orderId") Long orderId
    );

    @Query("""
        SELECT o.id
        FROM Orders o
    """)
    Page<Long> findAllOrderIds(Pageable pageable);

    @Query("""
        SELECT DISTINCT o
        FROM Orders o
        JOIN FETCH o.useraddresses ua
        LEFT JOIN FETCH o.pickupBranch
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.branch
        LEFT JOIN FETCH oi.products p
        LEFT JOIN FETCH p.brands
        LEFT JOIN FETCH p.categories
        WHERE o.id IN :ids
    """)
    List<Orders> findAllOrdersWithDetails(
            @Param("ids") List<Long> ids
    );

    @Query("""
        SELECT o.id
        FROM Orders o
        WHERE o.status = 'UNPAID'
          AND o.createdAt IS NOT NULL
          AND o.createdAt < :cutoff
    """)
    List<Long> findExpiredUnpaidOrderIds(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
        SELECT DISTINCT o
        FROM Orders o
        LEFT JOIN FETCH o.useraddresses ua
        LEFT JOIN FETCH o.pickupBranch
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.products p
        LEFT JOIN FETCH p.categories
        ORDER BY o.id DESC
    """)
    List<Orders> findAllWithItemsForDashboard();
}
