package com.challengehub.dto.request;

import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 100) String displayName,
        @Size(max = 500) String bio,
        @Size(max = 1000) String avatarUrl) {
}
