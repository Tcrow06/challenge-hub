package com.challengehub.dto.response;

import java.time.Instant;

public record CommentResponse(
        String id,
        String content,
        UserView user,
        Instant createdAt
) {
    public record UserView(
            String id,
            String username,
            String avatarUrl
    ) {
    }
}
