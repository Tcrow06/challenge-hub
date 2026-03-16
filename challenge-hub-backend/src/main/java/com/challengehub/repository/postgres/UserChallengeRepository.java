package com.challengehub.repository.postgres;

import com.challengehub.entity.postgres.UserChallengeEntity;
import com.challengehub.entity.postgres.Enums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserChallengeRepository extends JpaRepository<UserChallengeEntity, UUID> {
    Optional<UserChallengeEntity> findByUser_IdAndChallenge_Id(UUID userId, UUID challengeId);

    Optional<UserChallengeEntity> findByUser_IdAndChallenge_IdAndStatus(UUID userId, UUID challengeId, Enums.UserChallengeStatus status);

    boolean existsByUser_IdAndChallenge_Id(UUID userId, UUID challengeId);

    boolean existsByChallenge_Id(UUID challengeId);

    long countByChallenge_IdAndStatus(UUID challengeId, Enums.UserChallengeStatus status);

    List<UserChallengeEntity> findByChallenge_IdAndStatus(UUID challengeId, Enums.UserChallengeStatus status);
}
