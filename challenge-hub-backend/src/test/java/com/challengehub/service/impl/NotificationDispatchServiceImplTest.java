package com.challengehub.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.challengehub.entity.mongodb.NotificationDocument;
import com.challengehub.repository.mongodb.NotificationRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class NotificationDispatchServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationDispatchServiceImpl notificationDispatchService;

    @Test
    void createNotificationShouldPersistWhenNotExisting() {
        Map<String, Object> payload = Map.of(
                "title", "Bài nộp đã được duyệt",
                "message", "Bài nộp của bạn đạt 8/10 điểm",
                "metadata", Map.of("submission_id", "submission-1"));

        when(notificationRepository.existsByUserIdAndTypeAndReferenceId(
                "user-1",
                "SUBMISSION_APPROVED",
                "submission-1")).thenReturn(false);
        when(notificationRepository.save(any(NotificationDocument.class))).thenAnswer(invocation -> {
            NotificationDocument document = invocation.getArgument(0);
            document.setId("notif-1");
            return document;
        });

        boolean created = notificationDispatchService.createNotification(
                "user-1",
                "SUBMISSION_APPROVED",
                "submission-1",
                payload);

        assertThat(created).isTrue();

        ArgumentCaptor<NotificationDocument> documentCaptor = ArgumentCaptor.forClass(NotificationDocument.class);
        verify(notificationRepository, times(1)).save(documentCaptor.capture());

        NotificationDocument saved = documentCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.getType()).isEqualTo("SUBMISSION_APPROVED");
        assertThat(saved.getReferenceId()).isEqualTo("submission-1");
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getPayload()).containsKeys("title", "message", "metadata");

        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq("user-1"),
                eq("/queue/notifications"),
                any());
    }

    @Test
    void createNotificationShouldSkipWhenAlreadyExists() {
        Map<String, Object> payload = Map.of(
                "title", "Có người tham gia thử thách",
                "message", "Người dùng mới vừa tham gia challenge",
                "metadata", Map.of("challenge_id", "challenge-1", "participant_user_id", "user-2"));

        when(notificationRepository.existsByUserIdAndTypeAndReferenceId(
                "creator-1",
                "NEW_PARTICIPANT",
                "challenge-1:user-2")).thenReturn(true);

        boolean created = notificationDispatchService.createNotification(
                "creator-1",
                "NEW_PARTICIPANT",
                "challenge-1:user-2",
                payload);

        assertThat(created).isFalse();
        verify(notificationRepository, never()).save(any(NotificationDocument.class));
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void createNotificationShouldHandleDuplicateKeyRaceSafely() {
        Map<String, Object> payload = Map.of(
                "title", "Bài nộp đã được duyệt",
                "message", "Bài nộp của bạn đạt 8/10 điểm",
                "metadata", Map.of("submission_id", "submission-1"));

        when(notificationRepository.existsByUserIdAndTypeAndReferenceId(
                "user-1",
                "SUBMISSION_APPROVED",
                "submission-1")).thenReturn(false);
        when(notificationRepository.save(any(NotificationDocument.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        boolean created = notificationDispatchService.createNotification(
                "user-1",
                "SUBMISSION_APPROVED",
                "submission-1",
                payload);

        assertThat(created).isFalse();
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }
}
