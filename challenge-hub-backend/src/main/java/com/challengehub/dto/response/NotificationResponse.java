package com.challengehub.dto.response;

import java.time.Instant;
import java.util.Map;

public record NotificationResponse(
        String id,
        String type,
        String title,
        String message,
        Map<String, Object> metadata,
        boolean read,
        Instant createdAt
) {
}
