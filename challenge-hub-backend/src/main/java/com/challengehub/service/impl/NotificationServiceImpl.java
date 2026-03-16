package com.challengehub.service.impl;

import com.challengehub.dto.response.NotificationResponse;
import com.challengehub.entity.mongodb.NotificationDocument;
import com.challengehub.exception.ApiException;
import com.challengehub.repository.mongodb.NotificationRepository;
import com.challengehub.service.NotificationService;
import com.challengehub.service.SubmissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionService.PageResult<NotificationResponse> getNotifications(String currentUserId, boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(normalizePage(page) - 1, normalizeSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationDocument> notifications = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadOrderByCreatedAtDesc(currentUserId, false, pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUserId, pageable);

        Page<NotificationResponse> mapped = notifications.map(this::toResponse);
        return new SubmissionService.PageResult<>(
                mapped.getContent(),
                mapped.getNumber() + 1,
                mapped.getSize(),
                mapped.getTotalElements(),
                mapped.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String currentUserId) {
        return notificationRepository.countByRecipientIdAndRead(currentUserId, false);
    }

    @Override
    public void markRead(String notificationId, String currentUserId) {
        NotificationDocument doc = notificationRepository.findByIdAndRecipientId(notificationId, currentUserId)
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.NOT_FOUND, "Khong tim thay notification"));
        if (!doc.isRead()) {
            doc.setRead(true);
            notificationRepository.save(doc);
        }
    }

    @Override
    public void markAllRead(String currentUserId) {
        List<NotificationDocument> unread = notificationRepository.findByRecipientIdAndRead(currentUserId, false);
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

    private NotificationResponse toResponse(NotificationDocument doc) {
        return new NotificationResponse(
                doc.getId(),
                doc.getType(),
                doc.getTitle(),
                doc.getMessage(),
                doc.getMetadata(),
                doc.isRead(),
                doc.getCreatedAt()
        );
    }
}
