package com.challengehub.repository.postgres;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.SubmissionEntity;

import jakarta.persistence.LockModeType;

public interface SubmissionRepository extends JpaRepository<SubmissionEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SubmissionEntity s where s.id = :id")
    Optional<SubmissionEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<SubmissionEntity> findByTask_IdAndUser_Id(UUID taskId, UUID userId);

    List<SubmissionEntity> findByTask_IdInAndUser_Id(List<UUID> taskIds, UUID userId);

    List<SubmissionEntity> findByStatus(Enums.SubmissionStatus status);

    List<SubmissionEntity> findByUser_IdOrderBySubmittedAtDesc(UUID userId);

    List<SubmissionEntity> findByUser_IdAndStatusOrderBySubmittedAtDesc(UUID userId, Enums.SubmissionStatus status);

    List<SubmissionEntity> findByUser_IdAndTask_Challenge_IdOrderBySubmittedAtDesc(UUID userId, UUID challengeId);

    List<SubmissionEntity> findByUser_IdAndTask_Challenge_IdAndStatusOrderBySubmittedAtDesc(UUID userId,
            UUID challengeId, Enums.SubmissionStatus status);

    List<SubmissionEntity> findByStatusOrderBySubmittedAtAsc(Enums.SubmissionStatus status);

    List<SubmissionEntity> findByStatusAndTask_Challenge_IdOrderBySubmittedAtAsc(Enums.SubmissionStatus status,
            UUID challengeId);

    Page<SubmissionEntity> findByUser_Id(UUID userId, Pageable pageable);

    Page<SubmissionEntity> findByUser_IdAndStatus(UUID userId, Enums.SubmissionStatus status, Pageable pageable);

    Page<SubmissionEntity> findByUser_IdAndTask_Challenge_Id(UUID userId, UUID challengeId, Pageable pageable);

    Page<SubmissionEntity> findByUser_IdAndTask_Challenge_IdAndStatus(UUID userId, UUID challengeId,
            Enums.SubmissionStatus status, Pageable pageable);

    Page<SubmissionEntity> findByStatus(Enums.SubmissionStatus status, Pageable pageable);

    Page<SubmissionEntity> findByStatusAndTask_Challenge_Id(Enums.SubmissionStatus status, UUID challengeId,
            Pageable pageable);

    List<SubmissionEntity> findByTask_Challenge_IdAndStatus(UUID challengeId, Enums.SubmissionStatus status);

    long countByUser_Id(UUID userId);

    long countByTask_Challenge_IdAndUser_IdAndStatus(UUID challengeId, UUID userId, Enums.SubmissionStatus status);
}
