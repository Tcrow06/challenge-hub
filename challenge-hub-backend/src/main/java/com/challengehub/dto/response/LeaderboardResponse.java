package com.challengehub.dto.response;

import java.util.List;

public record LeaderboardResponse(
        String challengeId,
        List<RankingItem> rankings,
        MyRank myRank
) {
    public record RankingItem(
            long rank,
            UserView user,
            long totalScore,
            long tasksCompleted
    ) {
    }

    public record UserView(
            String id,
            String username,
            String avatarUrl
    ) {
    }

    public record MyRank(
            long rank,
            long totalScore,
            long tasksCompleted
    ) {
    }
}
