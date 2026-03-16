package com.challengehub.dto.response;

import com.challengehub.entity.postgres.Enums;

import java.time.Instant;

public record ChallengeResponse(
        String id,
        String title,
        String description,
        Enums.ChallengeStatus status,
        Enums.ChallengeDifficulty difficulty,
        String coverUrl,
        Instant startDate,
        Instant endDate,
        Integer maxParticipants,
        Boolean allowLateJoin,
        Enums.TaskUnlockMode taskUnlockMode,
        Integer taskCount,
        Long participantCount,
        CreatorView creator,
        Boolean isJoined,
        Instant createdAt,
        Instant updatedAt
) {
    public record CreatorView(
            String id,
            String username,
            String avatarUrl
    ) {
    }
}
