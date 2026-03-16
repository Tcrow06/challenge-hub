package com.challengehub.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String errorCode,
        List<FieldError> errors,
        Map<String, Object> metadata,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Thao tac thanh cong", data, null, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, null, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(T data, Map<String, Object> metadata) {
        return new ApiResponse<>(true, "Thao tac thanh cong", data, null, null, metadata, Instant.now());
    }

    public static <T> ApiResponse<T> success(T data, String message, Map<String, Object> metadata) {
        return new ApiResponse<>(true, message, data, null, null, metadata, Instant.now());
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, message, null, errorCode, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> validationError(List<FieldError> errors) {
        return new ApiResponse<>(false, "Du lieu khong hop le", null, "VALIDATION_FAILED", errors, null, Instant.now());
    }

    public record FieldError(String field, String message) {
    }
}
