package com.challengehub.repository.postgres;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.challengehub.entity.postgres.ChallengeEntity;
import com.challengehub.entity.postgres.Enums;

public interface ChallengeRepository extends JpaRepository<ChallengeEntity, UUID> {
    List<ChallengeEntity> findByStatus(Enums.ChallengeStatus status);

    List<ChallengeEntity> findByCreator_Id(UUID creatorId);

    List<ChallengeEntity> findAllByOrderByCreatedAtDesc();

    Page<ChallengeEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<ChallengeEntity> findByStatusAndStartDateLessThanEqual(Enums.ChallengeStatus status, Instant startDate);

    List<ChallengeEntity> findByStatusAndEndDateLessThanEqual(Enums.ChallengeStatus status, Instant endDate);
}
