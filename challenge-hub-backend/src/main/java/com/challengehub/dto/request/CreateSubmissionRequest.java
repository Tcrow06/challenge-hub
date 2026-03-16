package com.challengehub.dto.request;

import jakarta.validation.constraints.Size;

public record CreateSubmissionRequest(
        @Size(max = 2000) String description,
        String mediaId
) {
}
