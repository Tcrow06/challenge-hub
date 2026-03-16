package com.challengehub.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String customMessage;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    public ApiException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }

    public ApiException(String errorCode, String message, HttpStatus status) {
        super(message);
        ErrorCode resolved;
        try {
            resolved = ErrorCode.valueOf(errorCode);
        } catch (Exception ex) {
            resolved = ErrorCode.UNCATEGORIZED_ERROR;
        }
        this.errorCode = resolved;
        this.customMessage = message;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getEffectiveMessage() {
        return customMessage == null ? errorCode.getMessage() : customMessage;
    }
}
