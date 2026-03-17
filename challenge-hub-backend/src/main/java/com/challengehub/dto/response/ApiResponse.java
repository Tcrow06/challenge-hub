package com.challengehub.dto.response;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
        boolean success,
        T data,
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> meta,
        @JsonInclude(JsonInclude.Include.NON_NULL) ErrorBody error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return success(data);
    }

    public static <T> ApiResponse<T> success(T data, Map<String, Object> meta) {
        return new ApiResponse<>(true, data, meta, null);
    }

    public static <T> ApiResponse<T> success(T data, String message, Map<String, Object> meta) {
        return new ApiResponse<>(true, data, meta, null);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, null, null, new ErrorBody(errorCode, message, null));
    }

    public static <T> ApiResponse<T> validationError(List<FieldError> errors) {
        return new ApiResponse<>(
                false,
                null,
                null,
                new ErrorBody("VALIDATION_FAILED", "Du lieu khong hop le", errors));
    }

    public record ErrorBody(
            String code,
            String message,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<FieldError> details) {
    }

    public record FieldError(String field, String message) {
    }
}
