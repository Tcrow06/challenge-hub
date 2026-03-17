package com.challengehub.dto.request;

import com.challengehub.entity.postgres.Enums;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserStatusRequest(
        @NotNull Enums.UserStatus status,
        @Size(max = 500) String reason) {
}
