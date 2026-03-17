package com.challengehub.service.impl;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.challengehub.entity.mongodb.NotificationDocument;
import com.challengehub.repository.mongodb.NotificationRepository;
import com.challengehub.service.NotificationDispatchService;

@Service
public class NotificationDispatchServiceImpl implements NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchServiceImpl.class);
    private static final String USER_QUEUE_NOTIFICATIONS = "/queue/notifications";

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationDispatchServiceImpl(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public boolean createNotification(String userId, String type, String referenceId, Map<String, Object> payload) {
        String normalizedUserId = trimToNull(userId);
        String normalizedType = trimToNull(type);
        String normalizedReferenceId = trimToNull(referenceId);
        Map<String, Object> normalizedPayload = normalizePayload(payload);
        if (normalizedUserId == null
                || normalizedType == null
                || normalizedReferenceId == null
                || normalizedPayload == null) {
            return false;
        }

        if (notificationRepository.existsByUserIdAndTypeAndReferenceId(
                normalizedUserId,
                normalizedType,
                normalizedReferenceId)) {
            return false;
        }

        NotificationDocument notification = new NotificationDocument();
        notification.setUserId(normalizedUserId);
        notification.setType(normalizedType);
        notification.setReferenceId(normalizedReferenceId);
        notification.setPayload(normalizedPayload);
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());

        try {
            NotificationDocument savedNotification = notificationRepository.save(notification);
            pushRealtime(savedNotification);
            return true;
        } catch (DuplicateKeyException duplicateKeyException) {
            return false;
        }
    }

    private void pushRealtime(NotificationDocument notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                    Objects.requireNonNull(notification.getUserId()),
                    USER_QUEUE_NOTIFICATIONS,
                    Objects.requireNonNull(toRealtimePayload(notification)));
        } catch (RuntimeException exception) {
            log.warn("Failed to push notification realtime for userId={} notificationId={}",
                    notification.getUserId(),
                    notification.getId(),
                    exception);
        }
    }

    private Map<String, Object> toRealtimePayload(NotificationDocument notification) {
        Map<String, Object> payload = notification.getPayload() == null ? Map.of() : notification.getPayload();
        Map<String, Object> realtimePayload = new LinkedHashMap<>();
        realtimePayload.put("id", notification.getId());
        realtimePayload.put("type", notification.getType());
        realtimePayload.put("title", asString(payload.get("title")));
        realtimePayload.put("message", asString(payload.get("message")));
        realtimePayload.put("metadata", toMetadata(payload.get("metadata")));
        realtimePayload.put("read", notification.isRead());
        realtimePayload.put("createdAt", notification.getCreatedAt());
        return realtimePayload;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> normalizePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        String title = trimToNull(asString(payload.get("title")));
        String message = trimToNull(asString(payload.get("message")));
        Map<String, Object> metadata = toMetadata(payload.get("metadata"));

        if (title == null || message == null) {
            return null;
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("title", title);
        normalized.put("message", message);
        normalized.put("metadata", metadata);
        return normalized;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof String ? (String) value : String.valueOf(value);
    }

    private Map<String, Object> toMetadata(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            String normalizedKey = trimToNull(key);
            if (normalizedKey == null) {
                continue;
            }
            metadata.put(normalizedKey, entry.getValue());
        }
        return metadata;
    }
}
