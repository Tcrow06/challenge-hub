package com.challengehub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateChatChannelRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{2,30}$") String channelKey,
        @NotBlank @Size(min = 2, max = 50) String name,
        Boolean isReadonly) {
}
