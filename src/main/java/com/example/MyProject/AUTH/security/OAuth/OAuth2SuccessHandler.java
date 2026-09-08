package com.example.MyProject.AUTH.security.OAuth;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.entity.RefreshToken;
import com.example.MyProject.AUTH.repository.AccountRepository;
import com.example.MyProject.AUTH.security.jwt.JwtService;
import com.example.MyProject.AUTH.security.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AccountRepository accountRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauth2User =
                (OAuth2User) authentication.getPrincipal();

        String googleId = oauth2User.getAttribute("sub");

        if (googleId == null || googleId.isBlank()) {
            throw new RuntimeException("Không lấy được Google ID");
        }

        // ==============================
        // TÌM ACCOUNT
        // ==============================

        Account account = accountRepository
                .findByGoogleid(googleId)
                .orElseThrow(() ->
                        new RuntimeException("Account không tồn tại"));

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
        // SET REFRESH TOKEN COOKIE
        // ==============================

        ResponseCookie cookie =
                ResponseCookie.from(
                                "refreshToken",
                                refreshToken.getToken()
                        )
                        .httpOnly(true)
                        .secure(false)       // localhost HTTP
                        .path("/")
                        .domain("localhost")
                        .maxAge(7 * 24 * 60 * 60)
                        .sameSite("Lax")
                        .build();

        response.addHeader(
                "Set-Cookie",
                cookie.toString()
        );

        // ==============================
        // DEBUG
        // ==============================

        System.out.println("================================");
        System.out.println("GOOGLE LOGIN SUCCESS");
        System.out.println("Account ID: " + account.getId());
        System.out.println("Gmail: " + account.getGmail());
        System.out.println("Role: " + account.getRole());
        System.out.println("AccessToken created");
        System.out.println("RefreshToken created");
        System.out.println("================================");

        // ==============================
        // REDIRECT FRONTEND
        // ==============================

        String redirectUrl =
                "http://localhost:5500/trangchu.html"
                        + "#accessToken="
                        + accessToken;

        response.sendRedirect(redirectUrl);
    }
}