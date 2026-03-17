package com.challengehub.dto.response;

public record ChatDirectConversationResponse(
        String conversationId,
        String type,
        ChatConversationSummaryResponse.CounterpartView counterpart,
        boolean created) {
}
