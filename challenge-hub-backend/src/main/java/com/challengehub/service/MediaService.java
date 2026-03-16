package com.challengehub.service;

import com.challengehub.dto.request.GenerateUploadUrlRequest;
import com.challengehub.dto.response.GenerateUploadUrlResponse;
import com.challengehub.dto.response.MediaResponse;

public interface MediaService {

    GenerateUploadUrlResponse generateUploadUrl(GenerateUploadUrlRequest request, String currentUserId);

    MediaResponse confirmUpload(String mediaId, String currentUserId);

    void deleteMedia(String mediaId, String currentUserId, String currentRole);
}
