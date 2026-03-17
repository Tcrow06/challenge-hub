package com.challengehub.service;

import java.util.List;

import com.challengehub.dto.request.CreateChallengeRequest;
import com.challengehub.dto.request.CreateTaskRequest;
import com.challengehub.dto.request.UpdateChallengeStatusRequest;
import com.challengehub.dto.response.ChallengeDetailResponse;
import com.challengehub.dto.response.ChallengeListResponse;
import com.challengehub.dto.response.ChallengeParticipationResponse;
import com.challengehub.dto.response.TaskResponse;

public interface ChallengeService {

    PageResult<ChallengeListResponse> listChallenges(int page, int size);

    ChallengeDetailResponse getChallengeById(String challengeId, String currentUserId);

    ChallengeDetailResponse createChallenge(CreateChallengeRequest request, String currentUserId, String role);

    ChallengeDetailResponse updateChallengeStatus(String challengeId, UpdateChallengeStatusRequest request,
            String currentUserId, String role);

    ChallengeParticipationResponse joinChallenge(String challengeId, String currentUserId);

    ChallengeParticipationResponse quitChallenge(String challengeId, String currentUserId);

    TaskResponse createTask(String challengeId, CreateTaskRequest request, String currentUserId, String role);

    List<TaskResponse> getTasks(String challengeId, String currentUserId);

    record PageResult<T>(
            List<T> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }
}
