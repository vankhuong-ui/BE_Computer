package com.example.MyProject.ORDER.repository;

import com.example.MyProject.ORDER.entity.Payment;
import org.springframework.stereotype.Repository;


import com.example.MyProject.ORDER.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Lấy tất cả lịch sử giao dịch của 1 đơn hàng (cho Admin/Audit)
    List<Payment> findByOrderId(Long orderId);

    // Lấy 1 giao dịch mới nhất gần đây nhất của đơn hàng (SQL tự ORDER BY created_at DESC LIMIT 1)
    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);
}