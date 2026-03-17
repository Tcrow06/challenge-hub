package com.challengehub.repository.postgres;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.UserChallengeEntity;

import jakarta.persistence.LockModeType;

public interface UserChallengeRepository extends JpaRepository<UserChallengeEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select uc from UserChallengeEntity uc where uc.user.id = :userId and uc.challenge.id = :challengeId")
    Optional<UserChallengeEntity> findByUser_IdAndChallenge_IdForUpdate(@Param("userId") UUID userId,
            @Param("challengeId") UUID challengeId);

    Optional<UserChallengeEntity> findByUser_IdAndChallenge_Id(UUID userId, UUID challengeId);

    Optional<UserChallengeEntity> findByUser_IdAndChallenge_IdAndStatus(UUID userId, UUID challengeId,
            Enums.UserChallengeStatus status);

    long countByUser_Id(UUID userId);

    long countByUser_IdAndStatus(UUID userId, Enums.UserChallengeStatus status);

    boolean existsByUser_IdAndChallenge_Id(UUID userId, UUID challengeId);

    boolean existsByChallenge_Id(UUID challengeId);

    long countByChallenge_IdAndStatus(UUID challengeId, Enums.UserChallengeStatus status);

    List<UserChallengeEntity> findByChallenge_IdAndStatus(UUID challengeId, Enums.UserChallengeStatus status);
}
