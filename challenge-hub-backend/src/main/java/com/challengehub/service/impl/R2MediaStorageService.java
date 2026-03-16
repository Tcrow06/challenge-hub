package com.challengehub.service.impl;

import com.challengehub.config.StorageProperties;
import com.challengehub.service.MediaStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "media.provider", havingValue = "r2")
public class R2MediaStorageService implements MediaStorageService {

    private final StorageProperties storageProperties;

    public R2MediaStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public GenerateUploadUrlResult generateUploadUrl(String fileName, String contentType, Duration expiresIn) {
        String fileKey = "uploads/" + UUID.randomUUID() + "-" + fileName;
        String endpoint = storageProperties.getS3().getEndpoint();
        String bucket = storageProperties.getS3().getBucket();
        String uploadUrl = endpoint + "/" + bucket + "/" + fileKey;
        return new GenerateUploadUrlResult(uploadUrl, fileKey, expiresIn.getSeconds());
    }

    @Override
    public String getPublicUrl(String fileKey) {
        return storageProperties.getS3().getEndpoint() + "/" + storageProperties.getS3().getBucket() + "/" + fileKey;
    }

    @Override
    public void delete(String fileKey) {
        // TODO: call S3-compatible delete API for R2 in production implementation.
    }

    @Override
    public String providerName() {
        return "r2";
    }
}
