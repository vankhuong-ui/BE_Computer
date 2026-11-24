package com.example.MyProject.AUTH.service.impl;

import com.example.MyProject.AUTH.common.PhoneValidator;
import com.example.MyProject.AUTH.dto.address.request.UseraddressRequest;
import com.example.MyProject.AUTH.dto.address.response.UseraddressResponse;
import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.entity.Useraddresses;
import com.example.MyProject.AUTH.mapper.UseraddressMapper;
import com.example.MyProject.AUTH.repository.UseraddressRepository;
import com.example.MyProject.AUTH.service.UseraddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IUseraddressService implements UseraddressService {

    private final UseraddressMapper useraddressMapper;
    private final UseraddressRepository useraddressRepository;

    @Override
    @Transactional
    public void createUseraddress(UseraddressRequest useraddressRequest) {
        Account account = getCurrentAccount();
        List<Useraddresses> useraddressList = useraddressRepository.findByAccountId(account.getId());
        if (useraddressList.size() >= 5) {
            throw new RuntimeException("Mỗi tài khoản chỉ được đăng ký tối đa 5 địa chỉ nhận hàng");
        }
        String normalizedPhone = PhoneValidator.validateAndNormalize(useraddressRequest.phone());
        Useraddresses useraddresses = useraddressMapper.toEntity(useraddressRequest);
        useraddresses.setPhone(normalizedPhone);
        useraddresses.setAccount(account);
        useraddressRepository.save(useraddresses);
    }

    @Override
    @Transactional
    public void updateUseraddress(Long id, UseraddressRequest useraddressRequest) {
        Account account = getCurrentAccount();
        Useraddresses useraddresses = useraddressRepository.findByIdAndAccountId(id, account.getId())
                .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại hoặc không thuộc tài khoản của bạn"));
        String normalizedPhone = null;
        if (useraddressRequest.phone() != null) {
            normalizedPhone = PhoneValidator.validateAndNormalize(useraddressRequest.phone());
        }
        useraddressMapper.updateUseraddress(useraddressRequest, useraddresses);
        if (normalizedPhone != null) {
            useraddresses.setPhone(normalizedPhone);
        }
        useraddressRepository.save(useraddresses);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UseraddressResponse> showList(Pageable pageable) {
        return useraddressRepository.findAll(pageable).map(useraddressMapper::toRespon);
    }

    @Override
    @Transactional
    public void deleteUseraddress(Long id) {
        Account account = getCurrentAccount();
        Useraddresses useraddresses = useraddressRepository.findByIdAndAccountId(id, account.getId())
                .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại hoặc không thuộc tài khoản của bạn"));
        useraddressRepository.delete(useraddresses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UseraddressResponse> searchUseraddress(String nameUseraddress) {
        return useraddressRepository.findByUsername(nameUseraddress).stream()
                .map(useraddressMapper::toRespon)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UseraddressResponse> listaddress() {
        Account account = getCurrentAccount();
        return useraddressRepository.findByAccountId(account.getId()).stream()
                .map(useraddressMapper::toRespon)
                .toList();
    }

    private Account getCurrentAccount() {
        return (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
