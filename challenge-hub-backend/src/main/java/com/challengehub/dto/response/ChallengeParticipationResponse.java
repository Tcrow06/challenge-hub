package com.challengehub.dto.response;

import java.time.Instant;

import com.challengehub.entity.postgres.Enums;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChallengeParticipationResponse(
                String challengeId,
                String userId,
                Enums.UserChallengeStatus status,
                Instant joinedAt,
                Instant quitAt) {
}
