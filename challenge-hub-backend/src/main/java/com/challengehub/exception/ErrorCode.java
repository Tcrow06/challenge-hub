package com.challengehub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // ==================== 00xxx: System / Uncategorized ====================
    UNCATEGORIZED_ERROR(999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_SERVER_ERROR(500, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),

    // ==================== 01xxx: Validation (common) ====================
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),
    INVALID_INPUT(1002, "Invalid input", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(1003, "Validation failed", HttpStatus.BAD_REQUEST),
    INVALID_SORT_FIELD(1004, "Invalid sort field", HttpStatus.BAD_REQUEST),
    VALIDATION_DUPLICATE_EMAIL(1005, "Duplicate email", HttpStatus.CONFLICT),
    VALIDATION_DUPLICATE_USERNAME(1006, "Duplicate username", HttpStatus.CONFLICT),

    // ==================== 02xxx: Auth ====================
    AUTH_INVALID_CREDENTIALS(2001, "Invalid credentials", HttpStatus.UNAUTHORIZED),
    AUTH_ACCOUNT_LOCKED(2002, "Account locked", HttpStatus.LOCKED),
    AUTH_ACCOUNT_BANNED(2003, "Account banned", HttpStatus.FORBIDDEN),
    AUTH_ACCOUNT_SUSPENDED(2004, "Account suspended", HttpStatus.FORBIDDEN),
    AUTH_TOKEN_EXPIRED(2005, "Token expired", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID(2006, "Token invalid", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_REVOKED(2007, "Token revoked", HttpStatus.UNAUTHORIZED),
    AUTH_REFRESH_REPLAY(2008, "Refresh token replay detected", HttpStatus.UNAUTHORIZED),

    // ==================== 03xxx: Challenge ====================
    CHALLENGE_NOT_FOUND(3001, "Challenge not found", HttpStatus.NOT_FOUND),
    CHALLENGE_ALREADY_JOINED(3002, "Challenge already joined", HttpStatus.CONFLICT),
    CHALLENGE_FULL(3003, "Challenge full", HttpStatus.CONFLICT),
    CHALLENGE_NOT_JOINABLE(3004, "Challenge not joinable", HttpStatus.FORBIDDEN),
    CHALLENGE_NOT_JOINED(3005, "Challenge not joined", HttpStatus.BAD_REQUEST),
    CHALLENGE_ALREADY_QUIT(3006, "Challenge already quit", HttpStatus.CONFLICT),
    CHALLENGE_INVALID_TRANSITION(3007, "Invalid challenge transition", HttpStatus.BAD_REQUEST),
    CHALLENGE_HAS_PARTICIPANTS(3008, "Challenge has participants", HttpStatus.BAD_REQUEST),
    CHALLENGE_MISSING_TASKS(3009, "Challenge missing tasks", HttpStatus.BAD_REQUEST),
    CHALLENGE_MISSING_DATES(3010, "Challenge missing dates", HttpStatus.BAD_REQUEST),

    // ==================== 04xxx: Submission ====================
    SUBMISSION_NOT_FOUND(4001, "Submission not found", HttpStatus.NOT_FOUND),
    SUBMISSION_NOT_PARTICIPANT(4002, "User is not participant", HttpStatus.FORBIDDEN),
    SUBMISSION_ALREADY_EXISTS(4003, "Submission already exists", HttpStatus.CONFLICT),
    SUBMISSION_ALREADY_APPROVED(4004, "Submission already approved", HttpStatus.FORBIDDEN),
    SUBMISSION_CHALLENGE_ENDED(4005, "Challenge ended", HttpStatus.FORBIDDEN),
    SUBMISSION_INVALID_RESUBMIT(4006, "Invalid resubmit", HttpStatus.FORBIDDEN),
    SUBMISSION_SCORE_EXCEEDED(4007, "Submission score exceeded", HttpStatus.BAD_REQUEST),

    // ==================== 05xxx: Task ====================
    TASK_NOT_FOUND(5001, "Task not found", HttpStatus.NOT_FOUND),
    TASK_NOT_UNLOCKED(5002, "Task not unlocked", HttpStatus.FORBIDDEN),

    // ==================== 06xxx: Media ====================
    MEDIA_UPLOAD_FAILED(6001, "Media upload url generation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    MEDIA_TOO_LARGE(6002, "Media too large", HttpStatus.BAD_REQUEST),
    MEDIA_INVALID_TYPE(6003, "Invalid media type", HttpStatus.BAD_REQUEST),

    // ==================== 07xxx: Messaging ====================
    CHAT_CONVERSATION_NOT_FOUND(7001, "Conversation not found", HttpStatus.NOT_FOUND),
    CHAT_CHANNEL_NOT_FOUND(7002, "Channel not found", HttpStatus.NOT_FOUND),
    CHAT_CHANNEL_ALREADY_EXISTS(7003, "Channel already exists", HttpStatus.CONFLICT),
    CHAT_MEMBER_REQUIRED(7004, "Conversation membership required", HttpStatus.FORBIDDEN),
    CHAT_DM_SELF_NOT_ALLOWED(7005, "Direct message to self is not allowed", HttpStatus.BAD_REQUEST),
    CHAT_EMPTY_MESSAGE(7006, "Message content is empty", HttpStatus.BAD_REQUEST),
    CHAT_MESSAGE_NOT_FOUND(7007, "Message not found", HttpStatus.NOT_FOUND),
    CHAT_MESSAGE_EDIT_WINDOW_EXPIRED(7008, "Message edit window expired", HttpStatus.FORBIDDEN),
    CHAT_RATE_LIMITED(7009, "Chat rate limited", HttpStatus.TOO_MANY_REQUESTS),

    // ==================== 09xxx: General ====================
    FORBIDDEN(9001, "Forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND(9002, "Resource not found", HttpStatus.NOT_FOUND),
    RATE_LIMITED(9003, "Rate limited", HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(9004, "Internal error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
