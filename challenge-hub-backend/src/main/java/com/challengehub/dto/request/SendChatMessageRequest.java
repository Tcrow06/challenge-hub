package com.challengehub.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendChatMessageRequest(
        @Size(max = 2000) String content,
        List<@Valid AttachmentRequest> attachments) {

    public record AttachmentRequest(
            @NotBlank String mediaId) {
    }
}
