package com.example.MyProject.REVIEW.service;

import com.example.MyProject.REVIEW.dto.repair.RepairBookingRequest;
import com.example.MyProject.REVIEW.dto.repair.RepairBookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RepairBookingService {
    RepairBookingResponse createBooking(RepairBookingRequest request);
    Page<RepairBookingResponse> getMyBookings(Pageable pageable);
    Page<RepairBookingResponse> getAllBookings(Pageable pageable);
    RepairBookingResponse updateBookingStatus(Long id, String status, String adminNote);
}
