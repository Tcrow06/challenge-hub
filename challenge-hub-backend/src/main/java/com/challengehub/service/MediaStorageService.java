package com.challengehub.service;

import java.time.Duration;

public interface MediaStorageService {

    GenerateUploadUrlResult generateUploadUrl(String fileName, String contentType, Duration expiresIn);

    String getPublicUrl(String fileKey);

    void delete(String fileKey);

    String providerName();

    record GenerateUploadUrlResult(
            String uploadUrl,
            String fileKey,
            long expiresInSeconds
    ) {
    }
}
