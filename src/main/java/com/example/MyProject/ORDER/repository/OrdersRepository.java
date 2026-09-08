package com.example.MyProject.ORDER.repository;

import com.example.MyProject.ORDER.entity.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

    /*
     * ============================================================
     * LẤY ORDER THEO ACCOUNT
     * ============================================================
     */
    List<Orders> findByUseraddressesAccountId(Long accountId);


    /*
     * ============================================================
     * LẤY TẤT CẢ ORDER CỦA USER + FETCH CHI TIẾT
     *
     * Orders
     *   ├── Useraddresses
     *   └── OrderItems
     *         └── Products
     *               ├── Brands
     *               └── Categories
     * ============================================================
     */
    @Query("""
        SELECT DISTINCT o
        FROM Orders o
        JOIN FETCH o.useraddresses ua
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.products p
        JOIN FETCH p.brands
        JOIN FETCH p.categories
        WHERE ua.account.id = :accountId
    """)
    List<Orders> findOrdersWithDetailsByAccountId(
            @Param("accountId") Long accountId
    );


    /*
     * ============================================================
     * UPDATE TOTAL AMOUNT
     * ============================================================
     */
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


    /*
     * ============================================================
     * KIỂM TRA ORDER CÓ THUỘC USER KHÔNG
     * ============================================================
     */
    Optional<Orders> findByIdAndUseraddressesAccountId(
            Long orderId,
            Long accountId
    );


    /*
     * ============================================================
     * LẤY CHI TIẾT 1 ORDER CỦA USER
     *
     * Orders
     *   ├── Useraddresses
     *   └── OrderItems
     *         └── Products
     *               ├── Brands
     *               └── Categories
     * ============================================================
     */
    @Query("""
        SELECT DISTINCT o
        FROM Orders o
        JOIN FETCH o.useraddresses ua
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.products p
        JOIN FETCH p.brands
        JOIN FETCH p.categories
        WHERE o.id = :orderId
          AND ua.account.id = :accountId
    """)
    Optional<Orders> findByIdAndAccountIdWithDetails(
            @Param("orderId") Long orderId,
            @Param("accountId") Long accountId
    );


    /*
     * ============================================================
     * LẤY ID ORDER CÓ PHÂN TRANG
     *
     * KHÔNG viết ORDER BY trong JPQL.
     *
     * Pageable sẽ tự thêm:
     *
     * ORDER BY o1_0.created_at DESC
     *
     * ============================================================
     */
    @Query("""
        SELECT o.id
        FROM Orders o
    """)
    Page<Long> findAllOrderIds(Pageable pageable);


    /*
     * ============================================================
     * LẤY CHI TIẾT ORDER THEO LIST ID
     *
     * Dùng sau khi đã phân trang ID.
     *
     * ============================================================
     */
    @Query("""
        SELECT DISTINCT o
        FROM Orders o
        JOIN FETCH o.useraddresses ua
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.products p
        JOIN FETCH p.brands
        JOIN FETCH p.categories
        WHERE o.id IN :ids
    """)
    List<Orders> findAllOrdersWithDetails(
            @Param("ids") List<Long> ids
    );
}