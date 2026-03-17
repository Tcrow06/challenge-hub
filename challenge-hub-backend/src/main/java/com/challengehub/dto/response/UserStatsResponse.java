package com.challengehub.dto.response;

import java.time.Instant;
import java.util.List;

public record UserStatsResponse(
        long challengesJoined,
        long challengesCompleted,
        long submissionsCount,
        int currentStreak,
        List<BadgeView> badges) {

    public record BadgeView(
            String code,
            String name,
            String iconUrl,
            Instant earnedAt) {
    }
}
