package com.challengehub.repository.postgres;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.challengehub.entity.postgres.SubmissionScoreEventEntity;

public interface SubmissionScoreEventRepository extends JpaRepository<SubmissionScoreEventEntity, UUID> {
}