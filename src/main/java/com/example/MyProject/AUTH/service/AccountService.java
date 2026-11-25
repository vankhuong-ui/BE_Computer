package com.example.MyProject.AUTH.service;

import com.example.MyProject.AUTH.dto.auth.request.AccountRequest;
import com.example.MyProject.AUTH.dto.auth.request.LoginRequest;
import com.example.MyProject.AUTH.dto.auth.response.AccountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface AccountService {

    record AuthSessionResult(AccountResponse account, String refreshToken) {}

    void create(AccountRequest rq);
    void changeAccountStatus(Long id, Boolean enable);
    Page<AccountResponse> listAccount(Pageable page);
    List<AccountResponse> searchByEmail(String account, String email);
    void setGGPassword(String password, String newPassword);
    void seenGmail(String gmail);
    void checkCode(String gmail, String code);
    void saveNewPasswrod(String gmail, String code, String newPasswrod);
    boolean hasPassword();

    AuthSessionResult authenticateAndCreateSession(LoginRequest loginRequest);
    Map<String, String> refreshAccessToken(String refreshTokenStr);
    void logoutSession(String refreshTokenStr);
    AuthSessionResult verifyCodeAndCreateSession(String gmail, String code);
    AccountResponse resetForgotPassword(String gmail, String code, String newPassword);
}
