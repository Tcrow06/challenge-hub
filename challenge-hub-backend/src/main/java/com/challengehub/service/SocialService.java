package com.challengehub.service;

import com.challengehub.dto.response.ActivityFeedResponse;
import com.challengehub.dto.response.LeaderboardResponse;

public interface SocialService {

    LeaderboardResponse getLeaderboard(String challengeId, int top, String currentUserId);

    SubmissionService.PageResult<ActivityFeedResponse> getFeed(int page, int size);
}
