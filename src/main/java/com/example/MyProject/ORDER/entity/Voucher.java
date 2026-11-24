package com.example.MyProject.ORDER.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType = "FIXED";

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 19, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "min_order_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "first_order_only", nullable = false)
    private boolean firstOrderOnly = false;

    @Column(name = "usage_limit", nullable = false)
    private int usageLimit = 1000;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(nullable = false)
    private boolean active = true;
}
