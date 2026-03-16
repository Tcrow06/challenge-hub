package com.challengehub.repository.postgres;

import com.challengehub.entity.postgres.BadgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BadgeRepository extends JpaRepository<BadgeEntity, UUID> {
    Optional<BadgeEntity> findByCode(String code);
}
