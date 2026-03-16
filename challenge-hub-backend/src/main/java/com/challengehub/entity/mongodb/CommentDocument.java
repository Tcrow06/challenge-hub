package com.challengehub.entity.mongodb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document(collection = "comments")
public class CommentDocument {

    @Id
    private String id;

    private String submissionId;
    private String userId;
    private String username;
    private String avatarUrl;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
