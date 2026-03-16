package com.challengehub.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReactSubmissionRequest(
        @NotBlank String type
) {
}
