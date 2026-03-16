package com.challengehub.service.impl;

import com.challengehub.config.StorageProperties;
import com.challengehub.service.MediaStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "media.provider", havingValue = "cloudinary")
public class CloudinaryMediaStorageService implements MediaStorageService {

    private final StorageProperties storageProperties;

    public CloudinaryMediaStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public GenerateUploadUrlResult generateUploadUrl(String fileName, String contentType, Duration expiresIn) {
        String fileKey = "uploads/" + UUID.randomUUID() + "-" + fileName;
        String uploadUrl = "https://api.cloudinary.com/v1_1/" + storageProperties.getCloudinary().getCloudName() + "/auto/upload";
        return new GenerateUploadUrlResult(uploadUrl, fileKey, expiresIn.getSeconds());
    }

    @Override
    public String getPublicUrl(String fileKey) {
        return "https://res.cloudinary.com/" + storageProperties.getCloudinary().getCloudName() + "/image/upload/" + fileKey;
    }

    @Override
    public void delete(String fileKey) {
        // TODO: call Cloudinary destroy API in production implementation.
    }

    @Override
    public String providerName() {
        return "cloudinary";
    }
}
