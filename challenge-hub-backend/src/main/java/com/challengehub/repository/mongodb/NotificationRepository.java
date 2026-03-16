package com.challengehub.repository.mongodb;

import com.challengehub.entity.mongodb.NotificationDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
    List<NotificationDocument> findByRecipientIdAndReadOrderByCreatedAtDesc(String recipientId, boolean read);

    Page<NotificationDocument> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    Page<NotificationDocument> findByRecipientIdAndReadOrderByCreatedAtDesc(String recipientId, boolean read, Pageable pageable);

    Optional<NotificationDocument> findByIdAndRecipientId(String id, String recipientId);

    long countByRecipientIdAndRead(String recipientId, boolean read);

    List<NotificationDocument> findByRecipientIdAndRead(String recipientId, boolean read);
}
