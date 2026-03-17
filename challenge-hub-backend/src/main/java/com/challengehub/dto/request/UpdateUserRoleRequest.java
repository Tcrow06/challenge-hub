package com.challengehub.dto.request;

import com.challengehub.entity.postgres.Enums;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull Enums.UserRole role) {
}
