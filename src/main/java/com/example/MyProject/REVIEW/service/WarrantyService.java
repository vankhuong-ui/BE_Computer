package com.example.MyProject.REVIEW.service;

import com.example.MyProject.REVIEW.dto.warranty.WarrantyCardResponse;
import com.example.MyProject.REVIEW.dto.warranty.WarrantyClaimRequest;
import com.example.MyProject.REVIEW.dto.warranty.WarrantyClaimResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WarrantyService {
    List<WarrantyCardResponse> getMyWarrantyCards();
    List<WarrantyCardResponse> lookupWarranty(String keyword);
    WarrantyClaimResponse createClaim(WarrantyClaimRequest request);
    Page<WarrantyClaimResponse> getMyClaims(Pageable pageable);
    Page<WarrantyClaimResponse> getAllClaims(Pageable pageable);
    WarrantyClaimResponse updateClaimStatus(Long id, String status, String adminNote);
    long countPendingClaims();
}
