package com.challengehub.dto.request;

import com.challengehub.entity.postgres.Enums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateChallengeRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        Enums.ChallengeDifficulty difficulty,
        String coverUrl,
        Instant startDate,
        Instant endDate,
        Integer maxParticipants,
        Boolean allowLateJoin,
        @NotNull Enums.TaskUnlockMode taskUnlockMode
) {
}
