package com.example.MyProject.REVIEW.service.impl;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.PRODUCT.entity.Branch;
import com.example.MyProject.PRODUCT.service.BranchService;
import com.example.MyProject.REVIEW.dto.repair.RepairBookingRequest;
import com.example.MyProject.REVIEW.dto.repair.RepairBookingResponse;
import com.example.MyProject.REVIEW.entity.RepairBooking;
import com.example.MyProject.REVIEW.repository.RepairBookingRepository;
import com.example.MyProject.REVIEW.service.RepairBookingService;
import com.example.MyProject.common.PhoneValidator;
import com.example.MyProject.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RepairBookingServiceImpl implements RepairBookingService {

    private final RepairBookingRepository repairBookingRepository;
    private final BranchService branchService;

    @Transactional
    @Override
    public RepairBookingResponse createBooking(RepairBookingRequest request) {
        if (request == null) {
            throw new BusinessException("Thông tin đặt lịch không được trống");
        }
        if (request.customerName() == null || request.customerName().isBlank()) {
            throw new BusinessException("Vui lòng nhập họ tên khách hàng");
        }
        String normalizedPhone = PhoneValidator.validateAndNormalize(request.customerPhone());
        String method = request.serviceMethod() == null || request.serviceMethod().isBlank()
                ? "AT_STORE"
                : request.serviceMethod().trim().toUpperCase(Locale.ROOT);
        if (!List.of("AT_STORE", "AT_HOME").contains(method)) {
            throw new BusinessException("Hình thức sửa chữa không hợp lệ");
        }

        String resolvedBranch = request.branchName() != null ? request.branchName().trim() : "";
        String resolvedHomeAddr = request.homeAddress() != null ? request.homeAddress().trim() : "";

        if ("AT_HOME".equals(method)) {
            if (resolvedHomeAddr.isBlank()) {
                throw new BusinessException("Vui lòng nhập địa chỉ để kỹ thuật viên đến tận nơi");
            }
            resolvedBranch = "Sửa tại nhà — " + resolvedHomeAddr;
        } else {
            if (resolvedBranch.isBlank()) {
                throw new BusinessException("Vui lòng chọn chi nhánh tiếp nhận");
            }
            Branch branch = branchService.requireActiveBranchByCodeOrName(resolvedBranch);
            resolvedBranch = branch.getName();
        }

        if (request.deviceModel() == null || request.deviceModel().isBlank()) {
            throw new BusinessException("Vui lòng nhập tên dòng máy / thiết bị");
        }
        LocalDate appointmentDate = parseAppointmentDate(request.appointmentDate());
        LocalTime appointmentStart = parseAppointmentTime(request.appointmentTime());
        LocalDate today = LocalDate.now();
        if (appointmentDate.isBefore(today)
                || (appointmentDate.isEqual(today) && !appointmentStart.isAfter(LocalTime.now()))) {
            throw new BusinessException("Ngày và giờ hẹn phải nằm trong tương lai");
        }

        Account currentAccount = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Account acc) {
            currentAccount = acc;
        }

        RepairBooking booking = RepairBooking.builder()
                .account(currentAccount)
                .customerName(request.customerName().trim())
                .customerPhone(normalizedPhone)
                .customerEmail(request.customerEmail() != null && !request.customerEmail().isBlank()
                        ? request.customerEmail().trim()
                        : (currentAccount != null ? currentAccount.getGmail() : null))
                .serviceMethod(method)
                .branchName(resolvedBranch)
                .homeAddress(resolvedHomeAddr.isBlank() ? null : resolvedHomeAddr)
                .deviceModel(request.deviceModel().trim())
                .serviceType(request.serviceType() != null && !request.serviceType().isBlank()
                        ? request.serviceType().trim()
                        : "Kiểm tra & Sửa chữa tổng quát")
                .issueDescription(request.issueDescription() != null ? request.issueDescription().trim() : "")
                .appointmentDate(appointmentDate.toString())
                .appointmentTime(request.appointmentTime().trim().replace('–', '-'))
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
        String upper = status != null ? status.trim().toUpperCase(Locale.ROOT) : "";

        RepairBooking booking = repairBookingRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lịch hẹn sửa chữa #" + id));
        String currentStatus = booking.getStatus() != null ? booking.getStatus().trim().toUpperCase(Locale.ROOT) : "";
        if (!currentStatus.equals(upper) && !allowedBookingTransitions(currentStatus).contains(upper)) {
            throw new BusinessException("Không thể chuyển trạng thái lịch hẹn từ "
                    + currentStatus + " sang " + upper);
        }
        booking.setStatus(upper);
        if (adminNote != null) {
            booking.setAdminNote(adminNote.trim());
        }
        return toResponse(repairBookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingBookings() {
        return repairBookingRepository.findAll().stream()
                .filter(r -> r.getStatus() == null
                        || "PENDING".equalsIgnoreCase(r.getStatus())
                        || "CONFIRMED".equalsIgnoreCase(r.getStatus()))
                .count();
    }

    private LocalDate parseAppointmentDate(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Vui lòng chọn ngày hẹn");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("Ngày hẹn phải có định dạng yyyy-MM-dd");
        }
    }

    private LocalTime parseAppointmentTime(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Vui lòng chọn khung giờ hẹn");
        }
        String[] times = value.trim().replace('–', '-').split("\\s*-\\s*", -1);
        if (times.length < 1 || times.length > 2) {
            throw new BusinessException("Khung giờ phải có định dạng HH:mm hoặc HH:mm - HH:mm");
        }
        if (!times[0].matches("\\d{2}:\\d{2}")
                || (times.length == 2 && !times[1].matches("\\d{2}:\\d{2}"))) {
            throw new BusinessException("Khung giờ phải có định dạng HH:mm hoặc HH:mm - HH:mm");
        }
        try {
            LocalTime start = LocalTime.parse(times[0]);
            if (times.length == 2 && !LocalTime.parse(times[1]).isAfter(start)) {
                throw new BusinessException("Giờ kết thúc phải sau giờ bắt đầu");
            }
            return start;
        } catch (DateTimeParseException e) {
            throw new BusinessException("Khung giờ phải có định dạng HH:mm hoặc HH:mm - HH:mm");
        }
    }

    private List<String> allowedBookingTransitions(String currentStatus) {
        return switch (currentStatus) {
            case "PENDING" -> List.of("CONFIRMED", "CANCELLED");
            case "CONFIRMED" -> List.of("IN_PROGRESS", "CANCELLED");
            case "IN_PROGRESS" -> List.of("COMPLETED", "CANCELLED");
            default -> List.of();
        };
    }

    private RepairBookingResponse toResponse(RepairBooking b) {
        return new RepairBookingResponse(
                b.getId(),
                b.getAccount() != null ? b.getAccount().getId() : null,
                b.getCustomerName(),
                b.getCustomerPhone(),
                b.getCustomerEmail(),
                b.getServiceMethod(),
                b.getBranchName(),
                b.getHomeAddress(),
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
