package com.challengehub.repository.mongodb;

import com.challengehub.entity.mongodb.AuditLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogRepository extends MongoRepository<AuditLogDocument, String> {
}
