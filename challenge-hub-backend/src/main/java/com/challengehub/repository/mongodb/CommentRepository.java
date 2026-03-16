package com.challengehub.repository.mongodb;

import com.challengehub.entity.mongodb.CommentDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentRepository extends MongoRepository<CommentDocument, String> {
	Page<CommentDocument> findBySubmissionIdOrderByCreatedAtDesc(String submissionId, Pageable pageable);
}
