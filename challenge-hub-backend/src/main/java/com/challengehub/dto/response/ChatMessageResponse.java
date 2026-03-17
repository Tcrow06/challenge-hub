package com.challengehub.dto.response;

import java.time.Instant;
import java.util.List;

public record ChatMessageResponse(
        String id,
        String conversationId,
        String senderId,
        String type,
        String content,
        List<AttachmentView> attachments,
        Instant editedAt,
        boolean deleted,
        Instant createdAt) {

    public record AttachmentView(
            String mediaId,
            String fileUrl,
            String fileType,
            Long fileSize) {
    }
}
