package com.example.MyProject.ORDER.repository;

import com.example.MyProject.ORDER.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Voucher> findByActiveTrueOrderByFirstOrderOnlyDescIdAsc();
    List<Voucher> findAllByOrderByIdDesc();

    @Modifying
    @Query("""
        UPDATE Voucher v
        SET v.usedCount = v.usedCount + 1
        WHERE v.id = :id
          AND v.active = true
          AND v.usedCount < v.usageLimit
    """)
    int incrementUsageIfAvailable(@Param("id") Long id);

    @Modifying
    @Query("""
        UPDATE Voucher v
        SET v.usedCount = v.usedCount - 1
        WHERE v.id = :id
          AND v.usedCount > 0
    """)
    int decrementUsageIfPositive(@Param("id") Long id);
}
