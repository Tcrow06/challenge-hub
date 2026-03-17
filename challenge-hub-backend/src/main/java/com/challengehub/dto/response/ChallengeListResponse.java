package com.challengehub.dto.response;

import java.time.Instant;

import com.challengehub.entity.postgres.Enums;

public record ChallengeListResponse(
                String id,
                String title,
                String description,
                Enums.ChallengeStatus status,
                Instant startAt,
                Instant endAt,
                Long participantCount) {
}
