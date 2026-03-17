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
@CompoundIndex(name = "idx_notifications_user_read_created", def = "{'userId': 1, 'isRead': 1, 'createdAt': -1}")
@CompoundIndex(name = "uk_notifications_user_type_reference", def = "{'userId': 1, 'type': 1, 'referenceId': 1}", unique = true, partialFilter = "{'referenceId': {'$exists': true, '$type': 'string', '$ne': ''}}")
public class NotificationDocument {

    @Id
    private String id;

    private String userId;
    private String type;
    private Map<String, Object> payload;
    private String referenceId;
    private boolean isRead;

    @Indexed(expireAfter = "90d")
    private Instant createdAt;
}
