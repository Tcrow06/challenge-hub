package com.challengehub.service.impl;

import com.challengehub.dto.request.GenerateUploadUrlRequest;
import com.challengehub.dto.response.GenerateUploadUrlResponse;
import com.challengehub.dto.response.MediaResponse;
import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.MediaEntity;
import com.challengehub.entity.postgres.UserEntity;
import com.challengehub.exception.ApiException;
import com.challengehub.repository.postgres.MediaRepository;
import com.challengehub.repository.postgres.UserRepository;
import com.challengehub.service.MediaService;
import com.challengehub.service.MediaStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class MediaServiceImpl implements MediaService {

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 200L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "video/mp4"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".png", ".webp", ".mp4");

    private final MediaStorageService mediaStorageService;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;

    public MediaServiceImpl(MediaStorageService mediaStorageService,
                            MediaRepository mediaRepository,
                            UserRepository userRepository) {
        this.mediaStorageService = mediaStorageService;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
    }

    @Override
    public GenerateUploadUrlResponse generateUploadUrl(GenerateUploadUrlRequest request, String currentUserId) {
        validateRequest(request);

        UserEntity user = findUser(currentUserId);
        MediaStorageService.GenerateUploadUrlResult upload;
        try {
            upload = mediaStorageService.generateUploadUrl(request.fileName(), request.contentType(), Duration.ofMinutes(5));
        } catch (Exception ex) {
            throw new ApiException(com.challengehub.exception.ErrorCode.MEDIA_UPLOAD_FAILED, "Khong the tao upload url");
        }

        MediaEntity media = new MediaEntity();
        media.setUser(user);
        media.setStorageProvider(mediaStorageService.providerName());
        media.setFileKey(upload.fileKey());
        media.setFileType(request.contentType());
        media.setFileSize(request.fileSize());
        media.setStatus(Enums.MediaStatus.PENDING);
        media = mediaRepository.save(media);

        return new GenerateUploadUrlResponse(
                media.getId().toString(),
                upload.uploadUrl(),
                upload.fileKey(),
                upload.expiresInSeconds()
        );
    }

    @Override
    public MediaResponse confirmUpload(String mediaId, String currentUserId) {
        MediaEntity media = findMedia(mediaId);
        ensureOwner(media, currentUserId);

        media.setStatus(Enums.MediaStatus.CONFIRMED);
        media.setFileUrl(mediaStorageService.getPublicUrl(media.getFileKey()));
        media = mediaRepository.save(media);

        return new MediaResponse(media.getId().toString(), media.getFileUrl(), media.getFileType(), media.getFileSize());
    }

    @Override
    public void deleteMedia(String mediaId, String currentUserId, String currentRole) {
        MediaEntity media = findMedia(mediaId);
        boolean owner = media.getUser().getId().toString().equals(currentUserId);
        boolean admin = "ADMIN".equals(currentRole);
        if (!owner && !admin) {
            throw new ApiException(com.challengehub.exception.ErrorCode.FORBIDDEN, "Ban khong co quyen xoa media nay");
        }

        mediaStorageService.delete(media.getFileKey());
        mediaRepository.delete(media);
    }

    private void validateRequest(GenerateUploadUrlRequest request) {
        String contentType = request.contentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(com.challengehub.exception.ErrorCode.MEDIA_INVALID_TYPE, "Dinh dang file khong duoc ho tro");
        }

        String fileName = request.fileName().toLowerCase(Locale.ROOT);
        boolean extensionAllowed = ALLOWED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
        if (!extensionAllowed) {
            throw new ApiException(com.challengehub.exception.ErrorCode.MEDIA_INVALID_TYPE, "Duoi file khong duoc ho tro");
        }

        if (contentType.startsWith("image/") && request.fileSize() > MAX_IMAGE_SIZE) {
            throw new ApiException(com.challengehub.exception.ErrorCode.MEDIA_TOO_LARGE, "Kich thuoc anh vuot qua 10MB");
        }

        if (contentType.startsWith("video/") && request.fileSize() > MAX_VIDEO_SIZE) {
            throw new ApiException(com.challengehub.exception.ErrorCode.MEDIA_TOO_LARGE, "Kich thuoc video vuot qua 200MB");
        }
    }

    private UserEntity findUser(String currentUserId) {
        return userRepository.findById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.NOT_FOUND, "Khong tim thay nguoi dung"));
    }

    private MediaEntity findMedia(String mediaId) {
        return mediaRepository.findById(UUID.fromString(mediaId))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.NOT_FOUND, "Khong tim thay media"));
    }

    private void ensureOwner(MediaEntity media, String currentUserId) {
        if (!media.getUser().getId().toString().equals(currentUserId)) {
            throw new ApiException(com.challengehub.exception.ErrorCode.FORBIDDEN, "Ban khong co quyen truy cap media nay");
        }
    }
}
