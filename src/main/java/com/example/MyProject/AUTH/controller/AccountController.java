package com.example.MyProject.AUTH.controller;


import com.example.MyProject.AUTH.common.ApiResponse;
import com.example.MyProject.AUTH.common.BaseController;
import com.example.MyProject.AUTH.dto.auth.request.AccountRequest;
import com.example.MyProject.AUTH.dto.auth.request.LoginRequest;
import com.example.MyProject.AUTH.dto.auth.response.AccountResponse;
import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.entity.RefreshToken;
import com.example.MyProject.AUTH.mapper.AccountMapper;
import com.example.MyProject.AUTH.repository.AccountRepository;
import com.example.MyProject.AUTH.security.jwt.JwtService;
import com.example.MyProject.AUTH.security.service.RefreshTokenService;
import com.example.MyProject.AUTH.service.AccountService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/account")
public class AccountController extends BaseController {
    private final AccountService accountService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Page<AccountResponse>> showAccount(
            @RequestParam(name = "page_no", defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ApiResponse.succcess(accountService.listAccount(pageable));
    }

    @PutMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<String> updateAccount(@PathVariable("id") Long id, @RequestBody Boolean enabled) {
        accountService.changeAccountStatus(id, enabled);
        return createSuccessResponse("update enable success");
    }

    @GetMapping("/gmail")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<List<AccountResponse>> searchAccount(@RequestParam(required = false) String accountname, @RequestParam(required = false) String gmail) {
        List<AccountResponse> listrq = accountService.searchByEmail(accountname, gmail);
        return creatSucessTRessponsWithmessage("Đã tìm thấy Account!", listrq);
    }

    @PostMapping("/register")
    public ApiResponse<String> createAccount(@Valid @RequestBody AccountRequest accountRequest) {
        accountService.create(accountRequest);
        return createSuccessResponse("Create Account successs");
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AccountResponse>> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.gmail(),
                        loginRequest.password()
                )
        );

        Account account = (Account) authentication.getPrincipal();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(account.getId());

        // Dùng ResponseCookie để set SameSite và domain rõ ràng
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        System.out.println("Login - Token: " + refreshToken.getToken());
        System.out.println("Login - Cookie: " + cookie.toString());
        System.out.println("RefreshToken: " + refreshToken);
        return ResponseEntity.ok(ApiResponse.succcessWithmessage("Đăng nhập thành công",accountMapper.toResponse(account)));
    }

    @PostMapping("/refresh-token")
    public ApiResponse<Map<String, String>> refreshToken(
            @CookieValue(value = "refreshToken", required = false)
            String refreshTokenStr) {

        System.out.println("🔍 Refresh token from cookie: " + refreshTokenStr);

        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            throw new RuntimeException("Refresh token not found");
        }

        RefreshToken refreshToken =
                refreshTokenService.finByToken(refreshTokenStr)
                        .orElseThrow(() ->
                                new RuntimeException("Refresh token không tồn tại"));

        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh token đã bị thu hồi");
        }

        RefreshToken verifiedToken =
                refreshTokenService.verifyExpiration(refreshToken);

        Account account =
                verifiedToken.getAccount();

        String newAccessToken =
                jwtService.generateToken(account);

        return ApiResponse.succcess(
                Map.of(
                        "accessToken",
                        newAccessToken
                )
        );
    }

    @PostMapping("/logout")
    public ApiResponse<ResponseEntity<?>> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshTokenStr,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Hủy HttpSession và SecurityContext cũ để tránh lưu vết account trước
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }

        if (refreshTokenStr != null) {
            refreshTokenService.finByToken(refreshTokenStr)
                    .ifPresent(refreshToken -> {
                        refreshTokenService.revokeAllAcoountToken(refreshToken.getAccount().getId());
                    });
        }

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", deleteCookie.toString());

        return ApiResponse.succcess(new ResponseEntity<>(Map.of("message", "Logout Successfully"), HttpStatus.OK));
    }

    @PostMapping("/set-gg-password")
    public ApiResponse<String> setGGPassword(
            @RequestParam String password,
            @RequestParam String newPassword) {

        accountService.setGGPassword(password, newPassword);

        return createSuccessResponse(
                "Đổi mật khẩu thành công"
        );
    }

    @PostMapping("/send-code")
    public ApiResponse<String> sendCode(
            @RequestParam String gmail) {

        accountService.seenGmail(gmail);

        return createSuccessResponse(
                "Đã gửi mã OTP"
        );
    }

    @PostMapping("/check-code")
    public ApiResponse<AccountResponse> checkCode(
            @RequestParam String gmail,
            @RequestParam String code,
            HttpServletResponse response) {

        accountService.checkCode(gmail, code);
        Account account = accountRepository.findBygmail(gmail)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(account.getId());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("None")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        System.out.println("CheckCode - Cookie: " + cookie.toString());

        return creatSucessTRessponsWithmessage("Xác thực mã thành công",accountMapper.toResponse(account));
    }

    @PutMapping("/forgot-password")
    public ApiResponse<AccountResponse> forgotPassword(
            @RequestParam String gmail,
            @RequestParam String code,
            @RequestParam String newPassword) {

        accountService.saveNewPasswrod(
                gmail,
                code,
                newPassword
        );

        return creatSucessTRessponsWithmessage("Đổi mật khẩu thành công",accountMapper.toResponse(accountRepository.findBygmail(gmail).orElseThrow(()->new RuntimeException("gamil not found"))));
    }
    @GetMapping("/check-password")
    public ApiResponse<Map<String, Boolean>> checkPasswordExists() {
        boolean hasPassword = accountService.hasPassword();
        return creatSucessTRessponsWithmessage("Check password success!", Map.of("hasPassword", hasPassword));
    }
}
