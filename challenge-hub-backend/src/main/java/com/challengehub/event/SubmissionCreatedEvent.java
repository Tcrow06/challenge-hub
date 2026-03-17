package com.challengehub.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class SubmissionCreatedEvent extends DomainEvent {

    private final String userId;
    private final String submissionId;
    private final String challengeId;
    private final String taskId;
    private final String mediaId;

    public SubmissionCreatedEvent(String userId,
            String submissionId,
            String challengeId,
            String taskId,
            String mediaId) {
        super();
        this.userId = Objects.requireNonNull(userId);
        this.submissionId = Objects.requireNonNull(submissionId);
        this.challengeId = Objects.requireNonNull(challengeId);
        this.taskId = Objects.requireNonNull(taskId);
        this.mediaId = mediaId;
    }

    public SubmissionCreatedEvent(UUID eventId,
            Instant occurredAt,
            String userId,
            String submissionId,
            String challengeId,
            String taskId,
            String mediaId) {
        super(eventId, occurredAt);
        this.userId = Objects.requireNonNull(userId);
        this.submissionId = Objects.requireNonNull(submissionId);
        this.challengeId = Objects.requireNonNull(challengeId);
        this.taskId = Objects.requireNonNull(taskId);
        this.mediaId = mediaId;
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

    public String getMediaId() {
        return mediaId;
    }
}
