package com.challengehub.entity.mongodb;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "chat_conversations")
@CompoundIndex(name = "idx_chat_conversations_type_updated", def = "{'type': 1, 'updatedAt': -1}")
@CompoundIndex(name = "uk_chat_conversations_participants_hash", def = "{'participantsHash': 1}", unique = true, partialFilter = "{ 'type': 'DIRECT' }")
@CompoundIndex(name = "uk_chat_conversations_challenge_channel", def = "{'challengeId': 1, 'channelKey': 1}", unique = true, partialFilter = "{ 'type': 'CHALLENGE_CHANNEL' }")
public class ChatConversationDocument {

    @Id
    private String id;

    private String type;
    private String challengeId;
    private String channelKey;
    private String channelName;
    private boolean defaultChannel;
    private boolean readOnly;
    private String createdBy;
    private String participantsHash;
    private LastMessageSnapshot lastMessage;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean archived;

    @Getter
    @Setter
    public static class LastMessageSnapshot {
        private String messageId;
        private String senderId;
        private String contentPreview;
        private Instant sentAt;
    }
}
