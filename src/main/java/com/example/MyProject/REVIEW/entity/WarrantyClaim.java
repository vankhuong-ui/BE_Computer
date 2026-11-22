package com.example.MyProject.REVIEW.entity;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.PRODUCT.entity.Products;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "warranty_claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "serial_code", nullable = false, length = 100)
    private String serialCode;

    @Column(name = "issue_type", nullable = false, length = 100)
    private String issueType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Builder.Default
    @Column(name = "warranty_months", nullable = false)
    private Integer warrantyMonths = 12;

    @Column(name = "warranty_expire_date")
    private LocalDateTime warrantyExpireDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
