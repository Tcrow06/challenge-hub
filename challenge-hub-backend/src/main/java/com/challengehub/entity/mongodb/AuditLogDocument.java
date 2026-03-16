package com.challengehub.entity.mongodb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Document(collection = "audit_logs")
public class AuditLogDocument {

    @Id
    private String id;

    private String actorId;
    private String actorRole;
    private String action;
    private String resourceType;
    private String resourceId;
    private Map<String, Object> oldValue;
    private Map<String, Object> newValue;
    private String ipAddress;
    private String userAgent;
    private Instant timestamp;
}
