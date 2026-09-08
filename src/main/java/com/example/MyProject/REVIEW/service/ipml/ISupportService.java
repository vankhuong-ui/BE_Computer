package com.example.MyProject.REVIEW.service.ipml;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.REVIEW.dto.support.SupportResponse;
import com.example.MyProject.REVIEW.entity.Support;
import com.example.MyProject.REVIEW.mapper.SupportMapper;
import com.example.MyProject.REVIEW.repository.SupportRepository;
import com.example.MyProject.REVIEW.service.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ISupportService implements SupportService {

    private final SupportRepository supportRepository;
    private final SupportMapper supportMapper;

    @Transactional
    @Override
    public void createSupportRequest(Support support) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        support.setAccount(account);
        support.setStatus("OPEN");

        supportRepository.save(support);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<SupportResponse> getMySupportRequests(Pageable pageable) {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return supportRepository.findByAccountId(account.getId(), pageable)
                .map(supportMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<SupportResponse> getAllSupportRequests(Pageable pageable) {
        return supportRepository.findAll(pageable)
                .map(supportMapper::toResponse);
    }

    @Transactional
    @Override
    public void updateSupportStatus(Long id, String status) {
        Support support = supportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Yêu cầu hỗ trợ không tồn tại"));

        support.setStatus(status);
        supportRepository.save(support);
    }
}
