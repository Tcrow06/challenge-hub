package com.challengehub.repository.postgres;

import com.challengehub.entity.postgres.MediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MediaRepository extends JpaRepository<MediaEntity, UUID> {
}
