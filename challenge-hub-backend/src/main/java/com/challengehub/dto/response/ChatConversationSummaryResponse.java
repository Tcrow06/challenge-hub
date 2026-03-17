package com.challengehub.dto.response;

import java.time.Instant;

public record ChatConversationSummaryResponse(
        String id,
        String type,
        CounterpartView counterpart,
        ChallengeView challenge,
        ChannelView channel,
        LastMessageView lastMessage,
        long unreadCount,
        Instant updatedAt) {

    public record CounterpartView(
            String id,
            String username,
            String avatarUrl) {
    }

    public record ChallengeView(
            String id,
            String title) {
    }

    public record ChannelView(
            String key,
            String name,
            boolean isDefault) {
    }

    public record LastMessageView(
            String id,
            String senderId,
            String contentPreview,
            Instant sentAt) {
    }
}
