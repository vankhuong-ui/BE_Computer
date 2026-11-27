package com.example.MyProject.AUTH.service.impl;

import com.example.MyProject.AUTH.dto.address.request.UseraddressRequest;
import com.example.MyProject.AUTH.dto.address.response.UseraddressResponse;
import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.entity.Useraddresses;
import com.example.MyProject.AUTH.mapper.UseraddressMapper;
import com.example.MyProject.AUTH.repository.UseraddressRepository;
import com.example.MyProject.AUTH.service.UseraddressService;
import com.example.MyProject.common.PhoneValidator;
import com.example.MyProject.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UseraddressServiceImpl implements UseraddressService {

    private final UseraddressMapper useraddressMapper;
    private final UseraddressRepository useraddressRepository;

    @Override
    @Transactional
    public void createUseraddress(UseraddressRequest useraddressRequest) {
        Account account = getCurrentAccount();
        List<Useraddresses> useraddressList = useraddressRepository.findByAccountId(account.getId());
        if (useraddressList.size() >= 5) {
            throw new BusinessException("Mỗi tài khoản chỉ được đăng ký tối đa 5 địa chỉ nhận hàng");
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
        Useraddresses useraddresses = requireOwnedAddress(id, account.getId());
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
    @Transactional
    public void deleteUseraddress(Long id) {
        Account account = getCurrentAccount();
        Useraddresses useraddresses = requireOwnedAddress(id, account.getId());
        useraddressRepository.delete(useraddresses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UseraddressResponse> listaddress() {
        Account account = getCurrentAccount();
        return useraddressRepository.findByAccountId(account.getId()).stream()
                .map(useraddressMapper::toRespon)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Useraddresses requireOwnedAddress(Long addressId, Long accountId) {
        return useraddressRepository.findByIdAndAccountId(addressId, accountId)
                .orElseThrow(() -> new BusinessException("Địa chỉ không tồn tại hoặc không thuộc tài khoản của bạn"));
    }

    @Override
    @Transactional
    public Useraddresses resolveAddressForOrder(
            Account account,
            Long addressId,
            boolean isStorePickup,
            String receiverName,
            String receiverPhone,
            String pickupAddressSummary
    ) {
        Long accountId = account.getId();
        if (addressId != null) {
            return requireOwnedAddress(addressId, accountId);
        }
        if (isStorePickup) {
            List<Useraddresses> userAddrs = useraddressRepository.findByAccountId(accountId);
            if (!userAddrs.isEmpty()) {
                return userAddrs.stream()
                        .filter(a -> Boolean.TRUE.equals(a.getDefaultaddress()))
                        .findFirst()
                        .orElse(userAddrs.get(0));
            }
            Useraddresses autoAddr = new Useraddresses();
            autoAddr.setAccount(account);
            autoAddr.setUsername(receiverName != null && !receiverName.isBlank()
                    ? receiverName.trim()
                    : (account.getAccountname() != null ? account.getAccountname() : "Khách hàng"));
            autoAddr.setPhone(receiverPhone != null && !receiverPhone.isBlank()
                    ? PhoneValidator.validateAndNormalize(receiverPhone)
                    : "0900000000");
            autoAddr.setAddress(pickupAddressSummary);
            autoAddr.setDefaultaddress(true);
            return useraddressRepository.save(autoAddr);
        }
        throw new BusinessException("Vui lòng chọn địa chỉ giao hàng");
    }

    private Account getCurrentAccount() {
        return (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
