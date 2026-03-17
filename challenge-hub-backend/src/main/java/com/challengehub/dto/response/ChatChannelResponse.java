package com.challengehub.dto.response;

public record ChatChannelResponse(
        String conversationId,
        String channelKey,
        String name,
        boolean isDefault,
        boolean isReadonly,
        long memberCount) {
}
