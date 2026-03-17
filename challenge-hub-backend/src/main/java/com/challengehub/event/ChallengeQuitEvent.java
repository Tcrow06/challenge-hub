package com.challengehub.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ChallengeQuitEvent extends DomainEvent {

    private final String userId;
    private final String challengeId;
    private final String challengeCreatorId;
    private final String challengeTitle;

    public ChallengeQuitEvent(String userId,
            String challengeId,
            String challengeCreatorId,
            String challengeTitle) {
        super();
        this.userId = Objects.requireNonNull(userId);
        this.challengeId = Objects.requireNonNull(challengeId);
        this.challengeCreatorId = Objects.requireNonNull(challengeCreatorId);
        this.challengeTitle = Objects.requireNonNull(challengeTitle);
    }

    public ChallengeQuitEvent(UUID eventId,
            Instant occurredAt,
            String userId,
            String challengeId,
            String challengeCreatorId,
            String challengeTitle) {
        super(eventId, occurredAt);
        this.userId = Objects.requireNonNull(userId);
        this.challengeId = Objects.requireNonNull(challengeId);
        this.challengeCreatorId = Objects.requireNonNull(challengeCreatorId);
        this.challengeTitle = Objects.requireNonNull(challengeTitle);
    }

    public String getUserId() {
        return userId;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public String getChallengeCreatorId() {
        return challengeCreatorId;
    }

    public String getChallengeTitle() {
        return challengeTitle;
    }
}
