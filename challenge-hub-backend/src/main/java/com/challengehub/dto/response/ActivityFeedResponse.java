package com.challengehub.dto.response;

import java.time.Instant;

public record ActivityFeedResponse(
                String id,
                String userId,
                String type,
                String referenceId,
                UserView user,
                Instant createdAt) {
        public record UserView(
                        String id,
                        String username,
                        String avatarUrl) {
        }
}
