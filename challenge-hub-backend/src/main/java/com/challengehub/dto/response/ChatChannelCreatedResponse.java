package com.challengehub.dto.response;

import java.time.Instant;

public record ChatChannelCreatedResponse(
        String conversationId,
        String challengeId,
        String channelKey,
        String name,
        boolean isDefault,
        boolean isReadonly,
        Instant createdAt) {
}
