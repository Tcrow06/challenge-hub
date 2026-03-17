package com.challengehub.repository.mongodb;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.challengehub.entity.mongodb.ChatMembershipDocument;

public interface ChatMembershipRepository extends MongoRepository<ChatMembershipDocument, String> {

    List<ChatMembershipDocument> findByUserIdAndLeftAtIsNullOrderByUpdatedAtDesc(String userId);

    List<ChatMembershipDocument> findByConversationIdAndLeftAtIsNull(String conversationId);

    List<ChatMembershipDocument> findByConversationIdInAndLeftAtIsNull(List<String> conversationIds);

    List<ChatMembershipDocument> findByConversationIdAndLeftAtIsNullAndUserIdNot(String conversationId, String userId);

    Optional<ChatMembershipDocument> findByConversationIdAndUserId(String conversationId, String userId);

    Optional<ChatMembershipDocument> findByConversationIdAndUserIdAndLeftAtIsNull(String conversationId, String userId);

    long countByConversationIdAndLeftAtIsNull(String conversationId);
}
