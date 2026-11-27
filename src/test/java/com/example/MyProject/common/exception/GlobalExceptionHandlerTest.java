package com.example.MyProject.common.exception;

import com.example.MyProject.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void accessDeniedAndSecurityExceptionReturn403InsteadOf400() {
        ResponseEntity<ApiResponse<Object>> accessDeniedResp =
                handler.handleAccessDeniedException(new AccessDeniedException("Denied"));
        ResponseEntity<ApiResponse<Object>> securityResp =
                handler.handleSecurityException(new SecurityException("Không thuộc tài khoản"));

        assertThat(accessDeniedResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(accessDeniedResp.getBody()).isNotNull();
        assertThat(accessDeniedResp.getBody().code()).isEqualTo(403);

        assertThat(securityResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(securityResp.getBody()).isNotNull();
        assertThat(securityResp.getBody().code()).isEqualTo(403);
        assertThat(securityResp.getBody().message()).isEqualTo("Không thuộc tài khoản");
    }

    @Test
    void authenticationReturns401AndBusinessExceptionReturns400() {
        ResponseEntity<ApiResponse<Object>> authResp =
                handler.handleAuthenticationException(new BadCredentialsException("Sai thông tin đăng nhập"));
        ResponseEntity<ApiResponse<Object>> businessResp =
                handler.handleBusinessException(new BusinessException("Số lượng tồn kho không đủ"));

        assertThat(authResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(authResp.getBody()).isNotNull();
        assertThat(authResp.getBody().code()).isEqualTo(401);

        assertThat(businessResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(businessResp.getBody()).isNotNull();
        assertThat(businessResp.getBody().code()).isEqualTo(999);
        assertThat(businessResp.getBody().message()).isEqualTo("Số lượng tồn kho không đủ");
    }

    @Test
    void unexpectedInfrastructureRuntimeExceptionReturns500WithoutLeakingInternalDetails() {
        ResponseEntity<ApiResponse<Object>> systemResp =
                handler.handleUnexpectedException(new NullPointerException("Internal SQL connection pool NPE"));

        assertThat(systemResp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(systemResp.getBody()).isNotNull();
        assertThat(systemResp.getBody().code()).isEqualTo(500);
        assertThat(systemResp.getBody().message()).doesNotContain("NPE").doesNotContain("SQL");
    }
}
