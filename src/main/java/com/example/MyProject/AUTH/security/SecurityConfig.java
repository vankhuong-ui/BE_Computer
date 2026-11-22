package com.example.MyProject.AUTH.security;

import com.example.MyProject.AUTH.exception.CustomAccessDeniedHandler;
import com.example.MyProject.AUTH.exception.CustomAuthenticationEntryPoint;
import com.example.MyProject.AUTH.security.OAuth.OAuth2SuccessHandler;
import com.example.MyProject.AUTH.security.custom.CustomOAuth2UserService;
import com.example.MyProject.AUTH.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    @Lazy
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final UserDetailsService userDetailsService;

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    /**
     * ==============================
     * SECURITY FILTER CHAIN
     * ==============================
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ==============================
                // CSRF
                // ==============================
                .csrf(AbstractHttpConfigurer::disable)

                // ==============================
                // CORS
                // ==============================
                .cors(Customizer.withDefaults())

                // ==============================
                // SESSION
                // ==============================
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                // ==============================
                // AUTHORIZATION
                // ==============================
                .authorizeHttpRequests(auth -> auth

                        // --------------------------------
                        // WebSocket
                        // --------------------------------
                        .requestMatchers("/ws/**").permitAll()

                        // --------------------------------
                        // OAuth2
                        // --------------------------------
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/**"
                        ).permitAll()

                        // --------------------------------
                        // Account authentication
                        // --------------------------------
                        .requestMatchers(
                                "/api/account/login",
                                "/api/account/register",
                                "/api/account/send-code",
                                "/api/account/check-code",
                                "/api/account/forgot-password",
                                "/api/account/refresh-token"
                        ).permitAll()

                        // --------------------------------
                        // Public APIs
                        // --------------------------------
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/inventory/{id_product}/user",
                                "/api/brands",
                                "/api/brands/search",
                                "/api/categories",
                                "/api/reviews/product/{product_id}",
                                "/api/products",
                                "/api/products/search",
                                "/api/products/{id}/detailproduct",
                                "/api/products/*/product_brands",
                                "/api/products/*/product_categories",
                                "/api/products/*/*/product_categories_brands",
                                "/api/chat",
                                "/api/online",
                                "/api/payment/sepay-webhook",
                                "/api/warranty/lookup"
                        ).permitAll()

                        // --------------------------------
                        // Everything else requires login
                        // --------------------------------
                        .anyRequest().authenticated()
                )

                // ==============================
                // EXCEPTION HANDLING
                // ==============================
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                // ==============================
                // JWT FILTER
                // ==============================
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // ==============================
                // OAUTH2 LOGIN
                // ==============================
                .oauth2Login(oauth2 -> oauth2

                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )

                        .successHandler(oAuth2SuccessHandler)
                );

        return http.build();
    }

    /**
     * ==============================
     * CORS CONFIGURATION
     * ==============================
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5500",
                "http://127.0.0.1:5500",
                "https://hoang89computer.vercel.app"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        // Cho phép browser gửi HttpOnly refreshToken cookie
        configuration.setAllowCredentials(true);

        // Cache preflight request
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    /**
     * ==============================
     * PASSWORD ENCODER
     * ==============================
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * ==============================
     * AUTHENTICATION PROVIDER
     * ==============================
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    /**
     * ==============================
     * AUTHENTICATION MANAGER
     * ==============================
     */
    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http
    ) throws Exception {

        AuthenticationManagerBuilder builder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        builder.authenticationProvider(authenticationProvider());

        return builder.build();
    }
}
