package com.challengehub.service.impl;

import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.SubmissionEntity;
import com.challengehub.entity.postgres.SubmissionScoreEventEntity;
import com.challengehub.entity.postgres.UserChallengeEntity;
import com.challengehub.event.SubmissionApprovedEvent;
import com.challengehub.exception.ApiException;
import com.challengehub.repository.postgres.SubmissionRepository;
import com.challengehub.repository.postgres.SubmissionScoreEventRepository;
import com.challengehub.repository.postgres.TaskRepository;
import com.challengehub.repository.postgres.UserChallengeRepository;
import com.challengehub.service.SubmissionApprovedScoreService;

@Service
@Transactional
public class SubmissionApprovedScoreServiceImpl implements SubmissionApprovedScoreService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionScoreEventRepository submissionScoreEventRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final TaskRepository taskRepository;
    private final StringRedisTemplate redisTemplate;

    public SubmissionApprovedScoreServiceImpl(
            SubmissionRepository submissionRepository,
            SubmissionScoreEventRepository submissionScoreEventRepository,
            UserChallengeRepository userChallengeRepository,
            TaskRepository taskRepository,
            StringRedisTemplate redisTemplate) {
        this.submissionRepository = submissionRepository;
        this.submissionScoreEventRepository = submissionScoreEventRepository;
        this.userChallengeRepository = userChallengeRepository;
        this.taskRepository = taskRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean applyScore(SubmissionApprovedEvent event) {
        SubmissionEntity submission = submissionRepository
                .findByIdForUpdate(Objects.requireNonNull(UUID.fromString(event.getSubmissionId())))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.SUBMISSION_NOT_FOUND,
                        "Khong tim thay bai nop"));

        if (submission.getStatus() != Enums.SubmissionStatus.APPROVED || submission.getScore() == null) {
            return false;
        }

        if (!submission.getUser().getId().toString().equals(event.getUserId())
                || !submission.getTask().getChallenge().getId().toString().equals(event.getChallengeId())) {
            return false;
        }

        SubmissionScoreEventEntity scoreEvent = new SubmissionScoreEventEntity();
        scoreEvent.setSubmission(submission);
        try {
            submissionScoreEventRepository.save(scoreEvent);
        } catch (DataIntegrityViolationException duplicateEvent) {
            return false;
        }

        UUID userId = submission.getUser().getId();
        UUID challengeId = submission.getTask().getChallenge().getId();

        UserChallengeEntity userChallenge = userChallengeRepository
                .findByUser_IdAndChallenge_IdForUpdate(userId, challengeId)
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.SUBMISSION_NOT_PARTICIPANT,
                        "User khong thuoc challenge"));

        int score = Objects.requireNonNull(submission.getScore());
        int totalScore = userChallenge.getTotalScore() == null ? 0 : userChallenge.getTotalScore();
        userChallenge.setTotalScore(totalScore + score);
        updateChallengeProgress(userChallenge, challengeId, userId);
        userChallengeRepository.save(userChallenge);

        if (userChallenge.getStatus() != Enums.UserChallengeStatus.QUIT) {
            redisTemplate.opsForZSet().incrementScore(
                    "leaderboard:" + challengeId,
                    Objects.requireNonNull(userId.toString()),
                    score);
        }
        return true;
    }

    private void updateChallengeProgress(UserChallengeEntity userChallenge, UUID challengeId, UUID userId) {
        if (userChallenge.getStatus() == Enums.UserChallengeStatus.QUIT) {
            return;
        }

        long totalTasks = taskRepository.countByChallenge_Id(challengeId);
        if (totalTasks <= 0) {
            return;
        }

        long approvedTasks = submissionRepository.countByTask_Challenge_IdAndUser_IdAndStatus(
                challengeId,
                userId,
                Enums.SubmissionStatus.APPROVED);

        if (approvedTasks >= totalTasks) {
            userChallenge.setStatus(Enums.UserChallengeStatus.DONE);
            return;
        }

        if (userChallenge.getStatus() == Enums.UserChallengeStatus.DONE) {
            userChallenge.setStatus(Enums.UserChallengeStatus.ACTIVE);
        }
    }
}
