package com.challengehub.entity.mongodb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Document(collection = "notifications")
@CompoundIndex(name = "idx_notifications_recipient_read_created", def = "{'recipientId': 1, 'read': 1, 'createdAt': -1}")
public class NotificationDocument {

    @Id
    private String id;

    private String recipientId;
    private String type;
    private String title;
    private String message;
    private Map<String, Object> metadata;
    private boolean read;

    @Indexed(expireAfter = "90d")
    private Instant createdAt;
}
