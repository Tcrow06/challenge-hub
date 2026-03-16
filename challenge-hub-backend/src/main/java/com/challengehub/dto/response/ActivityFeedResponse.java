package com.challengehub.dto.response;

import java.time.Instant;
import java.util.Map;

public record ActivityFeedResponse(
        String id,
        String type,
        String message,
        UserView user,
        Map<String, Object> metadata,
        Instant createdAt
) {
    public record UserView(
            String id,
            String username,
            String avatarUrl
    ) {
    }
}
