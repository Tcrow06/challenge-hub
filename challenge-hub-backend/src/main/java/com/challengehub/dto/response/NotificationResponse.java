package com.challengehub.dto.response;

import java.time.Instant;
import java.util.Map;

public record NotificationResponse(
                String id,
                String userId,
                String type,
                Map<String, Object> payload,
                boolean isRead,
                Instant createdAt) {
}
