package com.challengehub.dto.response;

public record GenerateUploadUrlResponse(
        String mediaId,
        String uploadUrl,
        String fileKey,
        long expiresIn
) {
}
