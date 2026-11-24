package com.example.MyProject.ORDER.entity;

import com.example.MyProject.AUTH.entity.Useraddresses;
import com.example.MyProject.PRODUCT.entity.Branch;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "useraddresses_id")
    private Useraddresses useraddresses;

    @Column(name = "fulfillment_type", nullable = false, length = 30)
    private String fulfillmentType = "HOME_DELIVERY";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_branch_id")
    private Branch pickupBranch;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "receiver_phone", length = 50)
    private String receiverPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private BigDecimal totalAmount;

    private String status;

    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL
    )
    private List<OrderItem> orderItems = new ArrayList<>();
}
