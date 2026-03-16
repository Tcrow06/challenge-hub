package com.challengehub.repository.mongodb;

import com.challengehub.entity.mongodb.ActivityFeedDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActivityFeedRepository extends MongoRepository<ActivityFeedDocument, String> {
	Page<ActivityFeedDocument> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
