package com.challengehub.repository.mongodb;

import com.challengehub.entity.mongodb.NotificationDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
    List<NotificationDocument> findByUserIdAndIsReadOrderByCreatedAtDesc(String userId, boolean isRead);

    Page<NotificationDocument> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<NotificationDocument> findByUserIdAndIsReadOrderByCreatedAtDesc(String userId, boolean isRead,
            Pageable pageable);

    Optional<NotificationDocument> findByIdAndUserId(String id, String userId);

    long countByUserIdAndIsRead(String userId, boolean isRead);

    List<NotificationDocument> findByUserIdAndIsRead(String userId, boolean isRead);

    boolean existsByUserIdAndTypeAndReferenceId(String userId, String type, String referenceId);
}
