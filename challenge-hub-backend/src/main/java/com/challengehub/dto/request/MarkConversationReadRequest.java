package com.challengehub.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MarkConversationReadRequest(
        @NotBlank String lastMessageId) {
}
