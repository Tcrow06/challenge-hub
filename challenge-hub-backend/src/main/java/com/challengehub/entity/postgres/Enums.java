package com.challengehub.entity.postgres;

public final class Enums {

    private Enums() {
    }

    public enum UserRole {
        USER,
        CREATOR,
        MODERATOR,
        ADMIN
    }

    public enum UserStatus {
        ACTIVE,
        BANNED,
        SUSPENDED
    }

    public enum ChallengeStatus {
        DRAFT,
        PUBLISHED,
        ONGOING,
        ENDED,
        ARCHIVED
    }

    public enum ChallengeDifficulty {
        EASY,
        MEDIUM,
        HARD
    }

    public enum TaskUnlockMode {
        ALL_AT_ONCE,
        DAILY_UNLOCK
    }

    public enum UserChallengeStatus {
        ACTIVE,
        QUIT,
        DONE
    }

    public enum SubmissionStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum MediaStatus {
        PENDING,
        CONFIRMED
    }
}
