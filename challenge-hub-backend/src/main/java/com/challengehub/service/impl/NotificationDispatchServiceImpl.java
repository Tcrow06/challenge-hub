package com.challengehub.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
                || normalizedPayload.isEmpty()) {
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
        Map<String, Object> realtimePayload = new LinkedHashMap<>();
        realtimePayload.put("id", notification.getId());
        realtimePayload.put("userId", notification.getUserId());
        realtimePayload.put("type", notification.getType());
        realtimePayload.put("payload", notification.getPayload() == null ? Map.of() : notification.getPayload());
        realtimePayload.put("isRead", notification.isRead());
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
            return Map.of();
        }
        Map<String, Object> normalized = normalizeMap(payload);
        return normalized.isEmpty() ? Map.of() : normalized;
    }

    private Map<String, Object> normalizeMap(Map<?, ?> raw) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                continue;
            }
            String normalizedKey = normalizeKey(key);
            if (normalizedKey == null) {
                continue;
            }
            normalized.put(normalizedKey, normalizeValue(entry.getValue()));
        }
        return normalized;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof Map<?, ?> nestedMap) {
            return normalizeMap(nestedMap);
        }
        if (value instanceof List<?> list) {
            List<Object> normalizedList = new ArrayList<>(list.size());
            for (Object element : list) {
                normalizedList.add(normalizeValue(element));
            }
            return normalizedList;
        }
        return value;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder(trimmed.length());
        boolean uppercaseNext = false;
        for (char currentChar : trimmed.toCharArray()) {
            if (currentChar == '_' || currentChar == '-' || Character.isWhitespace(currentChar)) {
                uppercaseNext = true;
                continue;
            }
            if (builder.length() == 0) {
                builder.append(Character.toLowerCase(currentChar));
                uppercaseNext = false;
                continue;
            }
            if (uppercaseNext) {
                builder.append(Character.toUpperCase(currentChar));
                uppercaseNext = false;
            } else {
                builder.append(currentChar);
            }
        }

        return builder.isEmpty() ? null : builder.toString();
    }
}
