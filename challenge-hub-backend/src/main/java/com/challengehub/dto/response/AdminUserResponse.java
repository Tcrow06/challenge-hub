package com.challengehub.dto.response;

import java.time.Instant;

import com.challengehub.entity.postgres.Enums;

public record AdminUserResponse(
        String id,
        String username,
        String email,
        Enums.UserRole role,
        Enums.UserStatus status,
        Instant createdAt) {
}
