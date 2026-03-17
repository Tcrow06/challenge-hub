package com.challengehub.entity.mongodb;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "chat_messages")
@CompoundIndex(name = "idx_chat_messages_conversation_created", def = "{'conversationId': 1, 'createdAt': -1}")
@CompoundIndex(name = "idx_chat_messages_sender_created", def = "{'senderId': 1, 'createdAt': -1}")
public class ChatMessageDocument {

    @Id
    private String id;

    private String conversationId;
    private String senderId;
    private String senderUsername;
    private String senderAvatarUrl;
    private String type;
    private String content;
    private List<AttachmentSnapshot> attachments;
    private Instant editedAt;
    private Instant deletedAt;
    private Instant createdAt;

    @Getter
    @Setter
    public static class AttachmentSnapshot {
        private String mediaId;
        private String fileUrl;
        private String fileType;
        private Long fileSize;
    }
}
