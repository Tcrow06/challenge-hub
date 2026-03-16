package com.challengehub.entity.mongodb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Document(collection = "activity_feed")
public class ActivityFeedDocument {

    @Id
    private String id;

    private String userId;
    private String type;
    private String message;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
