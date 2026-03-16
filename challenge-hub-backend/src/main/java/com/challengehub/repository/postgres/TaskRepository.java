package com.challengehub.repository.postgres;

import com.challengehub.entity.postgres.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {
    List<TaskEntity> findByChallenge_Id(UUID challengeId);

    Optional<TaskEntity> findByChallenge_IdAndDayNumber(UUID challengeId, Integer dayNumber);
}
