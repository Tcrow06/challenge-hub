package com.challengehub.entity.mongodb;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "chat_memberships")
@CompoundIndex(name = "uk_chat_memberships_conversation_user", def = "{'conversationId': 1, 'userId': 1}", unique = true)
@CompoundIndex(name = "idx_chat_memberships_user_updated", def = "{'userId': 1, 'updatedAt': -1}")
public class ChatMembershipDocument {

    @Id
    private String id;

    private String conversationId;
    private String userId;
    private String role;
    private String lastReadMessageId;
    private Instant lastReadAt;
    private long unreadCount;
    private boolean muted;
    private Instant joinedAt;
    private Instant leftAt;
    private Instant updatedAt;
}
