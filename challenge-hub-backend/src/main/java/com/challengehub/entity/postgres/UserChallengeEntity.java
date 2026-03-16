package com.challengehub.entity.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user_challenges", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_challenge", columnNames = {"user_id", "challenge_id"})
})
public class UserChallengeEntity extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private ChallengeEntity challenge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.UserChallengeStatus status = Enums.UserChallengeStatus.ACTIVE;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();
}
