package com.challengehub.repository.postgres;

import com.challengehub.entity.postgres.ChallengeEntity;
import com.challengehub.entity.postgres.Enums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChallengeRepository extends JpaRepository<ChallengeEntity, UUID> {
    List<ChallengeEntity> findByStatus(Enums.ChallengeStatus status);

    List<ChallengeEntity> findByCreator_Id(UUID creatorId);

    List<ChallengeEntity> findAllByOrderByCreatedAtDesc();

    List<ChallengeEntity> findByStatusAndStartDateLessThanEqual(Enums.ChallengeStatus status, Instant startDate);

    List<ChallengeEntity> findByStatusAndEndDateLessThanEqual(Enums.ChallengeStatus status, Instant endDate);
}
