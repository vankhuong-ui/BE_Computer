package com.example.MyProject.AUTH.service;


import com.example.MyProject.AUTH.dto.auth.request.AccountRequest;
import com.example.MyProject.AUTH.dto.auth.response.AccountResponse;
import com.example.MyProject.AUTH.security.custom.CustomOAuth2UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AccountService {
    void create(AccountRequest rq);
    void changeAccountStatus(Long id,Boolean enable);
    Page<AccountResponse> listAccount(Pageable page);
    List<AccountResponse> searchByEmail(String account, String email);
    void setGGPassword(String password, String newPassword);
    void seenGmail(String gmail);
    void checkCode(String gamil,String code);
    void saveNewPasswrod(String gmail,String code,String newPasswrod);
    boolean hasPassword();
}
