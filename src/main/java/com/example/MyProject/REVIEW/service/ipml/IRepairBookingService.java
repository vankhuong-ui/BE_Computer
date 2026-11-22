package com.example.MyProject.REVIEW.service.ipml;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.REVIEW.dto.repair.RepairBookingRequest;
import com.example.MyProject.REVIEW.dto.repair.RepairBookingResponse;
import com.example.MyProject.REVIEW.entity.RepairBooking;
import com.example.MyProject.REVIEW.repository.RepairBookingRepository;
import com.example.MyProject.REVIEW.service.RepairBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IRepairBookingService implements RepairBookingService {

    private final RepairBookingRepository repairBookingRepository;

    @Transactional
    @Override
    public RepairBookingResponse createBooking(RepairBookingRequest request) {
        if (request == null) {
            throw new RuntimeException("Thông tin đặt lịch không được trống");
        }
        if (request.customerName() == null || request.customerName().isBlank()) {
            throw new RuntimeException("Vui lòng nhập họ tên khách hàng");
        }
        if (request.customerPhone() == null || request.customerPhone().isBlank()) {
            throw new RuntimeException("Vui lòng nhập số điện thoại liên hệ");
        }
        if (request.branchName() == null || request.branchName().isBlank()) {
            throw new RuntimeException("Vui lòng chọn chi nhánh tiếp nhận");
        }
        if (request.deviceModel() == null || request.deviceModel().isBlank()) {
            throw new RuntimeException("Vui lòng nhập tên dòng máy / thiết bị");
        }
        if (request.appointmentDate() == null || request.appointmentDate().isBlank()
                || request.appointmentTime() == null || request.appointmentTime().isBlank()) {
            throw new RuntimeException("Vui lòng chọn ngày và khung giờ hẹn");
        }

        Account currentAccount = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Account acc) {
            currentAccount = acc;
        }

        RepairBooking booking = RepairBooking.builder()
                .account(currentAccount)
                .customerName(request.customerName().trim())
                .customerPhone(request.customerPhone().trim())
                .customerEmail(request.customerEmail() != null && !request.customerEmail().isBlank()
                        ? request.customerEmail().trim()
                        : (currentAccount != null ? currentAccount.getGmail() : null))
                .branchName(request.branchName().trim())
                .deviceModel(request.deviceModel().trim())
                .serviceType(request.serviceType() != null && !request.serviceType().isBlank()
                        ? request.serviceType().trim()
                        : "Kiểm tra & Sửa chữa tổng quát")
                .issueDescription(request.issueDescription() != null ? request.issueDescription().trim() : "")
                .appointmentDate(request.appointmentDate().trim())
                .appointmentTime(request.appointmentTime().trim())
                .status("PENDING")
                .build();

        return toResponse(repairBookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<RepairBookingResponse> getMyBookings(Pageable pageable) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return repairBookingRepository.findByAccountId(account.getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<RepairBookingResponse> getAllBookings(Pageable pageable) {
        return repairBookingRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    @Override
    public RepairBookingResponse updateBookingStatus(Long id, String status, String adminNote) {
        List<String> valid = List.of("PENDING", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        String upper = status != null ? status.trim().toUpperCase() : "";
        if (!valid.contains(upper)) {
            throw new RuntimeException("Trạng thái lịch hẹn không hợp lệ: " + status);
        }

        RepairBooking booking = repairBookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn sửa chữa #" + id));
        booking.setStatus(upper);
        if (adminNote != null) {
            booking.setAdminNote(adminNote.trim());
        }
        return toResponse(repairBookingRepository.save(booking));
    }

    private RepairBookingResponse toResponse(RepairBooking b) {
        return new RepairBookingResponse(
                b.getId(),
                b.getAccount() != null ? b.getAccount().getId() : null,
                b.getCustomerName(),
                b.getCustomerPhone(),
                b.getCustomerEmail(),
                b.getBranchName(),
                b.getDeviceModel(),
                b.getServiceType(),
                b.getIssueDescription(),
                b.getAppointmentDate(),
                b.getAppointmentTime(),
                b.getStatus(),
                b.getAdminNote(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
