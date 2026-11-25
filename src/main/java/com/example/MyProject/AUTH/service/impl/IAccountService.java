package com.example.MyProject.AUTH.service.impl;

import com.example.MyProject.AUTH.dto.auth.request.AccountRequest;
import com.example.MyProject.AUTH.dto.auth.request.LoginRequest;
import com.example.MyProject.AUTH.dto.auth.response.AccountResponse;
import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.entity.RefreshToken;
import com.example.MyProject.AUTH.mapper.AccountMapper;
import com.example.MyProject.AUTH.repository.AccountRepository;
import com.example.MyProject.AUTH.security.jwt.JwtService;
import com.example.MyProject.AUTH.security.service.EmailVerificationService;
import com.example.MyProject.AUTH.security.service.RefreshTokenService;
import com.example.MyProject.AUTH.service.AccountService;
import com.example.MyProject.AUTH.service.RedisRateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IAccountService implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final RedisRateLimiterService rateLimiterService;
    @Lazy
    private final AuthenticationManager authenticationManager;

    @Override
    public void create(AccountRequest rq) {
        Account ac = accountMapper.toEntity(rq);
        if (accountRepository.existsBygmail(ac.getGmail())) {
            throw new RuntimeException("Tài khoản đã tồn tại");
        }
        ac.setPassword(passwordEncoder.encode(rq.password()));
        ac.setEnabled(false);
        ac.setRole("USER");
        accountRepository.save(ac);
    }

    @Override
    public void changeAccountStatus(Long id, Boolean enabled) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setEnabled(enabled);
        accountRepository.save(account);
    }

    @Override
    public Page<AccountResponse> listAccount(Pageable page) {
        Page<Account> accountPage = accountRepository.findAll(page);
        return accountPage.map(accountMapper::toResponse);
    }

    @Override
    public List<AccountResponse> searchByEmail(String accountname, String email) {
        List<Account> list = accountRepository.findByGmailOrAccountname(email, accountname);
        if (list.isEmpty()) {
            throw new RuntimeException("User not exist");
        }
        List<AccountResponse> listrq = new ArrayList<>();
        for (Account ac : list) {
            listrq.add(accountMapper.toResponse(ac));
        }
        return listrq;
    }

    @Override
    public void setGGPassword(String password, String newPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String gmail = authentication.getName();

        Account account = accountRepository.findBygmail(gmail)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));
        if (account.getPassword() == null) {
            account.setPassword(passwordEncoder.encode(newPassword));
            accountRepository.save(account);
        } else {
            if (!passwordEncoder.matches(password, account.getPassword())) {
                throw new RuntimeException("Password is false");
            }
            account.setPassword(passwordEncoder.encode(newPassword));
            accountRepository.save(account);
        }
    }

    @Override
    public void seenGmail(String gmail) {
        String normalizedEmail = gmail != null ? gmail.trim().toLowerCase() : "";
        if (!rateLimiterService.allowRequest("otp:" + normalizedEmail, 1, Duration.ofSeconds(60))) {
            throw new RuntimeException("Vui lòng đợi 60 giây trước khi yêu cầu gửi lại mã OTP");
        }
        emailVerificationService.sendOtp(gmail);
    }

    @Override
    public void checkCode(String gmail, String code) {
        if (emailVerificationService.verifyOtp(gmail, code)) {
            Account account = accountRepository.findBygmail(gmail)
                    .orElseThrow(() -> new RuntimeException("account not found"));
            account.setEnabled(true);
            accountRepository.save(account);
        } else {
            throw new RuntimeException("Code is false");
        }
    }

    @Override
    public void saveNewPasswrod(String gmail, String code, String newPasswrod) {
        if (emailVerificationService.verifyOtp(gmail, code)) {
            Account account = accountRepository.findBygmail(gmail)
                    .orElseThrow(() -> new RuntimeException("account not found"));
            account.setPassword(passwordEncoder.encode(newPasswrod));
            accountRepository.save(account);
        } else {
            throw new RuntimeException("Code is false");
        }
    }

    @Override
    public boolean hasPassword() {
        Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Account fullAccount = accountRepository.findById(account.getId())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));
        return fullAccount.getPassword() != null && !fullAccount.getPassword().isEmpty();
    }

    @Override
    public AuthSessionResult authenticateAndCreateSession(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.gmail(),
                        loginRequest.password()
                )
        );
        Account account = (Account) authentication.getPrincipal();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(account.getId());
        log.debug("Account login success: accountId={}", account.getId());
        return new AuthSessionResult(accountMapper.toResponse(account), refreshToken.getToken());
    }

    @Override
    public Map<String, String> refreshAccessToken(String refreshTokenStr) {
        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            throw new RuntimeException("Refresh token not found");
        }
        RefreshToken refreshToken = refreshTokenService.finByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Refresh token không tồn tại"));
        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh token đã bị thu hồi");
        }
        RefreshToken verifiedToken = refreshTokenService.verifyExpiration(refreshToken);
        Account account = verifiedToken.getAccount();
        String newAccessToken = jwtService.generateToken(account);
        return Map.of("accessToken", newAccessToken);
    }

    @Override
    public void logoutSession(String refreshTokenStr) {
        if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
            refreshTokenService.finByToken(refreshTokenStr)
                    .ifPresent(refreshToken ->
                            refreshTokenService.revokeAllAcoountToken(refreshToken.getAccount().getId())
                    );
        }
    }

    @Override
    public AuthSessionResult verifyCodeAndCreateSession(String gmail, String code) {
        checkCode(gmail, code);
        Account account = accountRepository.findBygmail(gmail)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(account.getId());
        log.debug("OTP verification success: accountId={}", account.getId());
        return new AuthSessionResult(accountMapper.toResponse(account), refreshToken.getToken());
    }

    @Override
    public AccountResponse resetForgotPassword(String gmail, String code, String newPassword) {
        saveNewPasswrod(gmail, code, newPassword);
        Account account = accountRepository.findBygmail(gmail)
                .orElseThrow(() -> new RuntimeException("gmail not found"));
        return accountMapper.toResponse(account);
    }
}
