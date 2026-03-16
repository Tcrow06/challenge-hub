package com.challengehub.dto.response;

import com.challengehub.entity.postgres.Enums;

import java.time.Instant;

public record SubmissionResponse(
        String id,
        TaskView task,
        ChallengeView challenge,
        String description,
        MediaView media,
        Enums.SubmissionStatus status,
        Integer score,
        String rejectReason,
        Instant submittedAt,
        Instant reviewedAt
) {
    public record TaskView(
            String id,
            String title,
            Integer dayNumber,
            Integer maxScore
    ) {
    }

    public record ChallengeView(
            String id,
            String title
    ) {
    }

    public record MediaView(
            String id,
            String fileUrl,
            String fileType
    ) {
    }
}
