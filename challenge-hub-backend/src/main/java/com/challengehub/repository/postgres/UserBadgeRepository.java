package com.challengehub.repository.postgres;

import com.challengehub.entity.postgres.UserBadgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserBadgeRepository extends JpaRepository<UserBadgeEntity, UUID> {
    boolean existsByUser_IdAndBadge_Id(UUID userId, UUID badgeId);

    List<UserBadgeEntity> findByUser_IdOrderByEarnedAtDesc(UUID userId);
}
