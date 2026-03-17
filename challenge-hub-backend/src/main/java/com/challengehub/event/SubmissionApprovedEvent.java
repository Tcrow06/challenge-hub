package com.challengehub.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class SubmissionApprovedEvent extends DomainEvent {

    private final String userId;
    private final String submissionId;
    private final String challengeId;
    private final String taskId;
    private final String reviewerId;
    private final int score;
    private final int maxScore;

    public SubmissionApprovedEvent(String userId,
            String submissionId,
            String challengeId,
            String taskId,
            String reviewerId,
            int score,
            int maxScore) {
        super();
        this.userId = Objects.requireNonNull(userId);
        this.submissionId = Objects.requireNonNull(submissionId);
        this.challengeId = Objects.requireNonNull(challengeId);
        this.taskId = Objects.requireNonNull(taskId);
        this.reviewerId = Objects.requireNonNull(reviewerId);
        this.score = score;
        this.maxScore = maxScore;
    }

    public SubmissionApprovedEvent(UUID eventId,
            Instant occurredAt,
            String userId,
            String submissionId,
            String challengeId,
            String taskId,
            String reviewerId,
            int score,
            int maxScore) {
        super(eventId, occurredAt);
        this.userId = Objects.requireNonNull(userId);
        this.submissionId = Objects.requireNonNull(submissionId);
        this.challengeId = Objects.requireNonNull(challengeId);
        this.taskId = Objects.requireNonNull(taskId);
        this.reviewerId = Objects.requireNonNull(reviewerId);
        this.score = score;
        this.maxScore = maxScore;
    }

    public String getUserId() {
        return userId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public int getScore() {
        return score;
    }

    public int getMaxScore() {
        return maxScore;
    }
}
