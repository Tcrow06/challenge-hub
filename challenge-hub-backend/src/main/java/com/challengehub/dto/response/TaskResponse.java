package com.challengehub.dto.response;

public record TaskResponse(
        String id,
        Integer dayNumber,
        String title,
        String content,
        Integer maxScore,
        boolean isUnlocked
) {
}
