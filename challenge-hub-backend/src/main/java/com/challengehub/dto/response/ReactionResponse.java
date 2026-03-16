package com.challengehub.dto.response;

import java.util.Map;

public record ReactionResponse(
        boolean reacted,
        String type,
        Map<String, Long> reactionCounts
) {
}
