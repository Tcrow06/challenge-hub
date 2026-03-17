package com.challengehub.service.impl;

import java.util.ArrayList;
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

    private NotificationResponse toResponse(NotificationDocument doc) {
        return new NotificationResponse(
                doc.getId(),
                doc.getUserId(),
                doc.getType(),
                normalizePayload(doc.getPayload()),
                doc.isRead(),
                doc.getCreatedAt());
    }
}
