package com.challengehub.dto.response;

import java.time.Instant;

import com.challengehub.entity.postgres.Enums;

public record SubmissionCreateResponse(
                String submissionId,
                String taskId,
                Enums.SubmissionStatus status,
                Instant createdAt) {
}
