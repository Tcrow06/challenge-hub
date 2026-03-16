package com.challengehub.repository.mongodb;

import com.challengehub.entity.mongodb.ReactionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ReactionRepository extends MongoRepository<ReactionDocument, String> {
    Optional<ReactionDocument> findBySubmissionIdAndUserId(String submissionId, String userId);

    long countBySubmissionIdAndType(String submissionId, String type);
}
