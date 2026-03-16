package com.challengehub.dto.response;

public record MediaResponse(
        String id,
        String fileUrl,
        String fileType,
        Long fileSize
) {
}
