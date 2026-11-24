package com.example.MyProject.AUTH.security.OAuth;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.entity.RefreshToken;
import com.example.MyProject.AUTH.repository.AccountRepository;
import com.example.MyProject.AUTH.security.jwt.JwtService;
import com.example.MyProject.AUTH.security.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AccountRepository accountRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @Value("${app.frontend-url:https://vankhuongdev.id.vn}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauth2User =
                (OAuth2User) authentication.getPrincipal();

        String googleId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");

        if (googleId == null || googleId.isBlank()) {
            throw new RuntimeException("Không lấy được Google ID");
        }

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Không lấy được Gmail");
        }

        // ==============================
        // TÌM ACCOUNT BẰNG GOOGLE ID
        // ==============================

        Account account = accountRepository
                .findByGoogleid(googleId)
                .orElse(null);

        // ==============================
        // NẾU CHƯA CÓ -> TÌM BẰNG GMAIL
        // ==============================

        if (account == null) {

            account = accountRepository
                    .findBygmail(email)
                    .orElse(null);
        }

        // ==============================
        // NẾU VẪN CHƯA CÓ -> TẠO ACCOUNT
        // ==============================

        if (account == null) {

            account = new Account();

            account.setGmail(email);

            String name = oauth2User.getAttribute("name");

            account.setAccountname(
                    name != null && !name.isBlank()
                            ? name
                            : email
            );

            account.setGoogleid(googleId);
            account.setRole("USER");
            account.setEnabled(true);
            account.setPassword(null);

            account = accountRepository.save(account);

        } else {

            // Nếu account tồn tại nhưng chưa có Google ID
            if (account.getGoogleid() == null
                    || account.getGoogleid().isBlank()) {

                account.setGoogleid(googleId);

                accountRepository.save(account);
            }
        }

        // ==============================
        // TẠO ACCESS TOKEN
        // ==============================

        String accessToken =
                jwtService.generateToken(account);

        // ==============================
        // TẠO REFRESH TOKEN
        // ==============================

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(account.getId());

        // ==============================
        // COOKIE
        // ==============================

        ResponseCookie cookie =
                ResponseCookie.from(
                                "refreshToken",
                                refreshToken.getToken()
                        )
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .maxAge(7 * 24 * 60 * 60)
                        .sameSite("None")
                        .build();

        response.addHeader(
                "Set-Cookie",
                cookie.toString()
        );

        log.info("Google OAuth2 login success: accountId={}, role={}", account.getId(), account.getRole());

        // ==============================
        // REDIRECT FRONTEND
        // ==============================

        String baseUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        String redirectUrl =
                baseUrl + "/trangchu.html"
                        + "#accessToken="
                        + accessToken;

        response.sendRedirect(redirectUrl);
    }
}
