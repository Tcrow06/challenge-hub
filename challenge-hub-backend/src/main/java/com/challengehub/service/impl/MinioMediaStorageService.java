package com.challengehub.service.impl;

import com.challengehub.config.StorageProperties;
import com.challengehub.service.MediaStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "media.provider", havingValue = "minio", matchIfMissing = true)
public class MinioMediaStorageService implements MediaStorageService {

    private final StorageProperties storageProperties;

    public MinioMediaStorageService(StorageProperties storageProperties) {
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
        // TODO: call S3 delete object API for MinIO in production implementation.
    }

    @Override
    public String providerName() {
        return "minio";
    }
}
