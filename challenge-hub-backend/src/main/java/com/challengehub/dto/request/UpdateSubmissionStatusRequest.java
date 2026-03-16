package com.challengehub.dto.request;

import com.challengehub.entity.postgres.Enums;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSubmissionStatusRequest(
        @NotNull Enums.SubmissionStatus status,
        @Min(0) @Max(100) Integer score,
        @Size(max = 500) String rejectReason
) {
}
