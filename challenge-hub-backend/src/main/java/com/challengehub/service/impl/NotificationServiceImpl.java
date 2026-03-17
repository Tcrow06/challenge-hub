package com.challengehub.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.challengehub.dto.response.NotificationResponse;
import com.challengehub.entity.mongodb.NotificationDocument;
import com.challengehub.exception.ApiException;
import com.challengehub.repository.mongodb.NotificationRepository;
import com.challengehub.service.NotificationService;
import com.challengehub.service.SubmissionService;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionService.PageResult<NotificationResponse> getNotifications(String currentUserId, boolean unreadOnly,
            int page, int size) {
        Pageable pageable = PageRequest.of(normalizePage(page) - 1, normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationDocument> notifications = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(currentUserId, false, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUserId, pageable);

        Page<NotificationResponse> mapped = notifications.map(this::toResponse);
        return new SubmissionService.PageResult<>(
                mapped.getContent(),
                mapped.getNumber() + 1,
                mapped.getSize(),
                mapped.getTotalElements(),
                mapped.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String currentUserId) {
        return notificationRepository.countByUserIdAndIsRead(currentUserId, false);
    }

    @Override
    public void markRead(String notificationId, String currentUserId) {
        NotificationDocument doc = notificationRepository.findByIdAndUserId(notificationId, currentUserId)
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.NOT_FOUND,
                        "Khong tim thay notification"));
        if (!doc.isRead()) {
            doc.setRead(true);
            notificationRepository.save(doc);
        }
    }

    @Override
    public void markAllRead(String currentUserId) {
        List<NotificationDocument> unread = Objects.requireNonNull(
                notificationRepository.findByUserIdAndIsRead(currentUserId, false));
        for (NotificationDocument doc : unread) {
            doc.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    private int normalizePage(int page) {
        return page < 1 ? 1 : page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private NotificationResponse toResponse(NotificationDocument doc) {
        Map<String, Object> payload = doc.getPayload() == null ? Map.of() : doc.getPayload();
        return new NotificationResponse(
                doc.getId(),
                doc.getType(),
                asString(payload.get("title")),
                asString(payload.get("message")),
                toMetadata(payload.get("metadata")),
                doc.isRead(),
                doc.getCreatedAt());
    }
}
