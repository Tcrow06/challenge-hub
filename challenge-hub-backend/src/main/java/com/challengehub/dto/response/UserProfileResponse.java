package com.challengehub.dto.response;

import java.time.Instant;

public record UserProfileResponse(
        String id,
        String username,
        String displayName,
        String avatarUrl,
        String bio,
        Instant createdAt) {
}
