package com.example.MyProject.REVIEW.repository;

import com.example.MyProject.REVIEW.entity.RepairBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepairBookingRepository extends JpaRepository<RepairBooking, Long> {
    Page<RepairBooking> findByAccountId(Long accountId, Pageable pageable);
}
