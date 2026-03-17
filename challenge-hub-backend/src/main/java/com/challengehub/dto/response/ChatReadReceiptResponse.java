package com.challengehub.dto.response;

import java.time.Instant;

public record ChatReadReceiptResponse(
        String conversationId,
        String lastReadMessageId,
        long unreadCount,
        Instant readAt) {
}
