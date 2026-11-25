package com.example.MyProject.AUTH.controller;

import com.example.MyProject.AUTH.common.ApiResponse;
import com.example.MyProject.AUTH.common.BaseController;
import com.example.MyProject.AUTH.dto.auth.request.AccountRequest;
import com.example.MyProject.AUTH.dto.auth.request.LoginRequest;
import com.example.MyProject.AUTH.dto.auth.response.AccountResponse;
import com.example.MyProject.AUTH.service.AccountService;
import com.example.MyProject.AUTH.service.AccountService.AuthSessionResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/account")
public class AccountController extends BaseController {

    private static final long REFRESH_COOKIE_MAX_AGE = 7L * 24 * 60 * 60;

    private final AccountService accountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ApiResponse<Page<AccountResponse>> showAccount(
            @RequestParam(name = "page_no", defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
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
    public ApiResponse<List<AccountResponse>> searchAccount(
            @RequestParam(required = false) String accountname,
            @RequestParam(required = false) String gmail
    ) {
        return creatSucessTRessponsWithmessage(
                "Đã tìm thấy Account!",
                accountService.searchByEmail(accountname, gmail)
        );
    }

    @PostMapping("/register")
    public ApiResponse<String> createAccount(@Valid @RequestBody AccountRequest accountRequest) {
        accountService.create(accountRequest);
        return createSuccessResponse("Create Account successs");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AccountResponse>> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {
        AuthSessionResult session = accountService.authenticateAndCreateSession(loginRequest);
        ResponseCookie cookie = buildRefreshCookie(session.refreshToken(), REFRESH_COOKIE_MAX_AGE);
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok(ApiResponse.succcessWithmessage("Đăng nhập thành công", session.account()));
    }

    @PostMapping("/refresh-token")
    public ApiResponse<Map<String, String>> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshTokenStr
    ) {
        return ApiResponse.succcess(accountService.refreshAccessToken(refreshTokenStr));
    }

    @PostMapping("/logout")
    public ApiResponse<ResponseEntity<?>> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshTokenStr,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }
        accountService.logoutSession(refreshTokenStr);
        ResponseCookie deleteCookie = buildRefreshCookie("", 0);
        response.addHeader("Set-Cookie", deleteCookie.toString());
        return ApiResponse.succcess(new ResponseEntity<>(Map.of("message", "Logout Successfully"), HttpStatus.OK));
    }

    @PostMapping("/set-gg-password")
    public ApiResponse<String> setGGPassword(
            @RequestParam String password,
            @RequestParam String newPassword
    ) {
        accountService.setGGPassword(password, newPassword);
        return createSuccessResponse("Đổi mật khẩu thành công");
    }

    @PostMapping("/send-code")
    public ApiResponse<String> sendCode(@RequestParam String gmail) {
        accountService.seenGmail(gmail);
        return createSuccessResponse("Đã gửi mã OTP");
    }

    @PostMapping("/check-code")
    public ApiResponse<AccountResponse> checkCode(
            @RequestParam String gmail,
            @RequestParam String code,
            HttpServletResponse response
    ) {
        AuthSessionResult session = accountService.verifyCodeAndCreateSession(gmail, code);
        ResponseCookie cookie = buildRefreshCookie(session.refreshToken(), REFRESH_COOKIE_MAX_AGE);
        response.addHeader("Set-Cookie", cookie.toString());
        return creatSucessTRessponsWithmessage("Xác thực mã thành công", session.account());
    }

    @PutMapping("/forgot-password")
    public ApiResponse<AccountResponse> forgotPassword(
            @RequestParam String gmail,
            @RequestParam String code,
            @RequestParam String newPassword
    ) {
        AccountResponse account = accountService.resetForgotPassword(gmail, code, newPassword);
        return creatSucessTRessponsWithmessage("Đổi mật khẩu thành công", account);
    }

    @GetMapping("/check-password")
    public ApiResponse<Map<String, Boolean>> checkPasswordExists() {
        boolean hasPassword = accountService.hasPassword();
        return creatSucessTRessponsWithmessage("Check password success!", Map.of("hasPassword", hasPassword));
    }

    private ResponseCookie buildRefreshCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("None")
                .build();
    }
}
