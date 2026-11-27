package com.example.MyProject.AUTH.service;

import com.example.MyProject.AUTH.dto.address.request.UseraddressRequest;
import com.example.MyProject.AUTH.dto.address.response.UseraddressResponse;
import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.entity.Useraddresses;

import java.util.List;

public interface UseraddressService {
    void createUseraddress(UseraddressRequest useraddressRequest);
    void updateUseraddress(Long id, UseraddressRequest useraddressRequest);
    void deleteUseraddress(Long id);
    List<UseraddressResponse> listaddress();

    Useraddresses requireOwnedAddress(Long addressId, Long accountId);
    Useraddresses resolveAddressForOrder(
            Account account,
            Long addressId,
            boolean isStorePickup,
            String receiverName,
            String receiverPhone,
            String pickupAddressSummary
    );
}
