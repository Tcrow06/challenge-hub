package com.challengehub.entity.mongodb;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "activity_feed")
@CompoundIndex(name = "idx_activity_feed_user_created", def = "{'userId': 1, 'createdAt': -1}")
@CompoundIndex(name = "uk_activity_feed_user_type_reference", def = "{'userId': 1, 'type': 1, 'referenceId': 1}", unique = true, partialFilter = "{'referenceId': {'$exists': true, '$type': 'string', '$ne': ''}}")
public class ActivityFeedDocument {

    @Id
    private String id;

    private String userId;
    private String type;
    private String referenceId;
    private Instant createdAt;
}
