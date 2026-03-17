package com.challengehub.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateChatMessageRequest(
        @Size(max = 2000) String content) {
}
