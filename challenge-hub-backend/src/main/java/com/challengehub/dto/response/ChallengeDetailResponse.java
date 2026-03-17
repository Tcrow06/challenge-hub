package com.challengehub.dto.response;

import java.time.Instant;

import com.challengehub.entity.postgres.Enums;

public record ChallengeDetailResponse(
                String id,
                String title,
                String description,
                Enums.ChallengeStatus status,
                Enums.ChallengeDifficulty difficulty,
                String coverUrl,
                Instant startAt,
                Instant endAt,
                Integer maxParticipants,
                Boolean allowLateJoin,
                Enums.TaskUnlockMode taskUnlockMode,
                Integer taskCount,
                Long participantCount,
                CreatorView creator,
                Boolean isJoined,
                Instant createdAt,
                Instant updatedAt) {
        public record CreatorView(
                        String id,
                        String username,
                        String avatarUrl) {
        }
}
