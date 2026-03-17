package com.challengehub.repository.mongodb;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.challengehub.entity.mongodb.ChatMessageDocument;

public interface ChatMessageRepository extends MongoRepository<ChatMessageDocument, String> {

    Optional<ChatMessageDocument> findByIdAndConversationId(String id, String conversationId);

    List<ChatMessageDocument> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    List<ChatMessageDocument> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(String conversationId,
            Instant createdAt,
            Pageable pageable);
}
