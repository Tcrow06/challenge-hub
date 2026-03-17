package com.challengehub.repository.mongodb;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.challengehub.entity.mongodb.AuditLogDocument;

public interface AuditLogRepository extends MongoRepository<AuditLogDocument, String> {

    Page<AuditLogDocument> findAllByOrderByTimestampDesc(Pageable pageable);
}
