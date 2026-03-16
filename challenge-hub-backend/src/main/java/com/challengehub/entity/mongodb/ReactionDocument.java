package com.challengehub.entity.mongodb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document(collection = "reactions")
@CompoundIndex(name = "uk_reaction_submission_user", def = "{'submissionId': 1, 'userId': 1}", unique = true)
public class ReactionDocument {

    @Id
    private String id;

    private String submissionId;
    private String userId;
    private String type;
    private Instant createdAt;
}
