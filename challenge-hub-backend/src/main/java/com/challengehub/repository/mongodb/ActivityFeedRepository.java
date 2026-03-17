package com.challengehub.repository.mongodb;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.challengehub.entity.mongodb.ActivityFeedDocument;

public interface ActivityFeedRepository extends MongoRepository<ActivityFeedDocument, String> {
	boolean existsByUserIdAndTypeAndReferenceId(String userId, String type, String referenceId);

	Page<ActivityFeedDocument> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
