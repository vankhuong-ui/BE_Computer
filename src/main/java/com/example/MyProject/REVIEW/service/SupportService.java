package com.example.MyProject.REVIEW.service;

import com.example.MyProject.REVIEW.dto.support.SupportResponse;
import com.example.MyProject.REVIEW.entity.Support;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface SupportService {
    void createSupportRequest(Support support);
    Page<SupportResponse> getMySupportRequests(Pageable pageable);
    Page<SupportResponse> getAllSupportRequests(Pageable pageable);
    void updateSupportStatus(Long id, String status);
}