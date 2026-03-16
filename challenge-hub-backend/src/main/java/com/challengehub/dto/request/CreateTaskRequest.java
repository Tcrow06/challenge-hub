package com.challengehub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank @Size(max = 255) String title,
        String content,
        @NotNull @Min(1) Integer dayNumber,
        @NotNull @Min(1) @Max(100) Integer maxScore
) {
}
