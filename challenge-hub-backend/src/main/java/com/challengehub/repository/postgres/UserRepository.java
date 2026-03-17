package com.challengehub.repository.postgres;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.challengehub.entity.postgres.UserEntity;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
