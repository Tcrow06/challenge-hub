package com.challengehub.dto.response;

import java.time.Instant;

public record ChatMessageEditResponse(
        String id,
        String conversationId,
        String content,
        Instant editedAt) {
}
