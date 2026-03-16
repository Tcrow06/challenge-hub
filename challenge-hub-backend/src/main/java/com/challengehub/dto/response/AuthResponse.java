package com.challengehub.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserView user
) {
    public record UserView(
            String id,
            String username,
            String email,
            String role,
            String avatarUrl,
            String displayName
    ) {
    }
}
