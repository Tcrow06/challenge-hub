package com.challengehub.entity.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "challenges")
public class ChallengeEntity extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private UserEntity creator;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.ChallengeStatus status = Enums.ChallengeStatus.DRAFT;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Enums.ChallengeDifficulty difficulty;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "allow_late_join", nullable = false)
    private Boolean allowLateJoin = Boolean.TRUE;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_unlock_mode", nullable = false, length = 20)
    private Enums.TaskUnlockMode taskUnlockMode = Enums.TaskUnlockMode.ALL_AT_ONCE;
}
