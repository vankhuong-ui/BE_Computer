package com.example.MyProject.REVIEW.repository;

import com.example.MyProject.REVIEW.entity.Support;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportRepository extends JpaRepository<Support, Long> {

    /**
     * Lấy danh sách yêu cầu hỗ trợ phân trang theo accountId của khách hàng
     */
    Page<Support> findByAccountId(Long accountId, Pageable pageable);
}