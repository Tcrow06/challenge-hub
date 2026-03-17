package com.challengehub.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
                String id,
                String username,
                String email,
                String role,
                Instant createdAt,
                String accessToken,
                String tokenType,
                Long expiresIn,
                UserView user) {

        public static AuthResponse register(
                        String id,
                        String username,
                        String email,
                        String role,
                        Instant createdAt) {
                return new AuthResponse(
                                id,
                                username,
                                email,
                                role,
                                createdAt,
                                null,
                                null,
                                null,
                                null);
        }

        public static AuthResponse login(
                        String accessToken,
                        String tokenType,
                        long expiresIn,
                        UserView user) {
                return new AuthResponse(
                                null,
                                null,
                                null,
                                null,
                                null,
                                accessToken,
                                tokenType,
                                expiresIn,
                                user);
        }

        public static AuthResponse refresh(
                        String accessToken,
                        String tokenType,
                        long expiresIn) {
                return new AuthResponse(
                                null,
                                null,
                                null,
                                null,
                                null,
                                accessToken,
                                tokenType,
                                expiresIn,
                                null);
        }

        public record UserView(
                        String id,
                        String username,
                        String email,
                        String role,
                        String avatarUrl,
                        String displayName) {
        }
}
