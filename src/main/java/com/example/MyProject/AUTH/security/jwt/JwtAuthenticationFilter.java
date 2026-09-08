package com.example.MyProject.AUTH.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String requestUri = request.getRequestURI();

        // ==========================================
        // BỎ QUA OAUTH2 LOGIN
        // ==========================================

        if (requestUri.startsWith("/oauth2/")
                || requestUri.startsWith("/login/")) {

            filterChain.doFilter(request, response);
            return;
        }

        // ==========================================
        // LẤY AUTHORIZATION HEADER
        // ==========================================

        final String authHeader =
                request.getHeader("Authorization");

        // Không có Bearer Token
        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        final String jwt =
                authHeader.substring(7).trim();

        if (jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            // ==========================================
            // LẤY GMAIL TỪ JWT
            // ==========================================

            String username =
                    jwtService.extractUsername(jwt);

            if (username == null || username.isBlank()) {
                filterChain.doFilter(request, response);
                return;
            }

            // ==========================================
            // LOAD ACCOUNT
            // ==========================================

            UserDetails userDetails =
                    userDetailsService
                            .loadUserByUsername(username);

            // ==========================================
            // KIỂM TRA JWT
            // ==========================================

            if (jwtService.validateToken(
                    jwt,
                    userDetails
            )) {

                UsernamePasswordAuthenticationToken
                        authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                // ==========================================
                // QUAN TRỌNG:
                // JWT LUÔN GHI ĐÈ OAuth2 Authentication
                // ==========================================

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authenticationToken);

                log.debug(
                        "JWT authentication successful: {}",
                        username
                );
            }

        } catch (Exception e) {

            log.error(
                    "JWT authentication failed: {}",
                    e.getMessage()
            );

            // Xóa Authentication cũ nếu JWT không hợp lệ
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(null);
        }

        filterChain.doFilter(request, response);
    }
}