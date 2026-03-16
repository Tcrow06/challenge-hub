package com.challengehub.service;

import com.challengehub.dto.response.NotificationResponse;

public interface NotificationService {

    SubmissionService.PageResult<NotificationResponse> getNotifications(String currentUserId, boolean unreadOnly, int page, int size);

    long getUnreadCount(String currentUserId);

    void markRead(String notificationId, String currentUserId);

    void markAllRead(String currentUserId);
}
