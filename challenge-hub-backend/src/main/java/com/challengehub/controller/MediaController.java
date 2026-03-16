package com.challengehub.controller;

import com.challengehub.dto.request.GenerateUploadUrlRequest;
import com.challengehub.dto.response.ApiResponse;
import com.challengehub.dto.response.GenerateUploadUrlResponse;
import com.challengehub.dto.response.MediaResponse;
import com.challengehub.service.MediaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/upload-url")
    public ResponseEntity<ApiResponse<GenerateUploadUrlResponse>> generateUploadUrl(
            @Valid @RequestBody GenerateUploadUrlRequest request,
            Authentication authentication
    ) {
        GenerateUploadUrlResponse response = mediaService.generateUploadUrl(request, currentUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/confirm/{mediaId}")
    public ResponseEntity<ApiResponse<MediaResponse>> confirmUpload(
            @PathVariable String mediaId,
            Authentication authentication
    ) {
        MediaResponse response = mediaService.confirmUpload(mediaId, currentUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable String mediaId,
            Authentication authentication
    ) {
        mediaService.deleteMedia(mediaId, currentUserId(authentication), currentUserRole(authentication));
        return ResponseEntity.ok(ApiResponse.success(null, "Xoa media thanh cong"));
    }

    private String currentUserId(Authentication authentication) {
        return String.valueOf(authentication.getPrincipal());
    }

    private String currentUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
    }
}
