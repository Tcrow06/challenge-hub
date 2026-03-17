package com.challengehub.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.challengehub.dto.response.ActivityFeedResponse;
import com.challengehub.dto.response.ApiResponse;
import com.challengehub.dto.response.LeaderboardResponse;
import com.challengehub.service.SocialService;
import com.challengehub.service.SubmissionService;

@RestController
@RequestMapping("/api/v1/social")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping("/leaderboard/{challengeId}")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getLeaderboard(
            @PathVariable String challengeId,
            @RequestParam(name = "top", defaultValue = "50") int top,
            Authentication authentication) {
        String currentUserId = authentication == null ? null : String.valueOf(authentication.getPrincipal());
        LeaderboardResponse response = socialService.getLeaderboard(challengeId, top, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<ActivityFeedResponse>>> getFeed(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        SubmissionService.PageResult<ActivityFeedResponse> result = socialService.getFeed(page, size);
        return ResponseEntity.ok(ApiResponse.success(result.items(), Map.of(
                "page", result.page(),
                "size", result.size(),
                "totalElements", result.totalElements(),
                "totalPages", result.totalPages())));
    }
}
