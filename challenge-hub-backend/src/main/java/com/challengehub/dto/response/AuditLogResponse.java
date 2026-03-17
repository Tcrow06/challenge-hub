package com.challengehub.dto.response;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditLogResponse(
        String id,
        String actorId,
        String actorRole,
        String action,
        String targetType,
        String targetId,
        Map<String, Object> oldValue,
        Map<String, Object> newValue,
        String ipAddress,
        String userAgent,
        Instant createdAt) {
}
