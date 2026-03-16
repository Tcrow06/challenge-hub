package com.challengehub.service;

import com.challengehub.dto.request.CreateChallengeRequest;
import com.challengehub.dto.request.CreateTaskRequest;
import com.challengehub.dto.request.UpdateChallengeStatusRequest;
import com.challengehub.dto.response.ChallengeResponse;
import com.challengehub.dto.response.TaskResponse;

import java.util.List;

public interface ChallengeService {

    List<ChallengeResponse> listChallenges();

    ChallengeResponse getChallengeById(String challengeId, String currentUserId);

    ChallengeResponse createChallenge(CreateChallengeRequest request, String currentUserId, String role);

    ChallengeResponse updateChallengeStatus(String challengeId, UpdateChallengeStatusRequest request, String currentUserId, String role);

    void joinChallenge(String challengeId, String currentUserId);

    void quitChallenge(String challengeId, String currentUserId);

    TaskResponse createTask(String challengeId, CreateTaskRequest request, String currentUserId, String role);

    List<TaskResponse> getTasks(String challengeId, String currentUserId);
}
