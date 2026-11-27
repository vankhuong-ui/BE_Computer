package com.example.MyProject.common;

public abstract class BaseController {

    protected <T> ApiResponse<T> successResponse(String message) {
        return ApiResponse.successMessage(message);
    }

    protected <T> ApiResponse<T> successDataResponse(String message, T result) {
        return ApiResponse.successData(message, result);
    }

    protected <T> ApiResponse<T> createSuccessResponse(T data) {
        return ApiResponse.successData(data);
    }

    protected <T> ApiResponse<T> createSuccessWithMessage(String message, T data) {
        return ApiResponse.successData(message, data);
    }
}
