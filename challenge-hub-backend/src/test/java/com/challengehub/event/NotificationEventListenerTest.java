package com.challengehub.event;

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

import com.challengehub.service.NotificationDispatchService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "null", "unchecked" })
class NotificationEventListenerTest {

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    void submissionApprovedEventShouldNotifySubmissionOwner() {
        SubmissionApprovedEvent event = new SubmissionApprovedEvent(
                "user-1",
                "submission-1",
                "challenge-1",
                "task-1",
                "reviewer-1",
                8,
                10);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = mapCaptor();

        listener.onSubmissionApproved(event);

        verify(notificationDispatchService, times(1)).createNotification(
                eq("user-1"),
                eq("SUBMISSION_APPROVED"),
                eq("submission-1"),
                payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload)
                .containsEntry("title", "Bài nộp đã được duyệt")
                .containsEntry("submissionId", "submission-1")
                .containsEntry("challengeId", "challenge-1")
                .containsEntry("taskId", "task-1")
                .containsEntry("reviewerId", "reviewer-1")
                .containsEntry("score", 8)
                .containsEntry("maxScore", 10)
                .containsKeys("message", "eventId", "occurredAt");
    }

    @Test
    void challengeJoinedEventShouldNotifyChallengeCreator() {
        ChallengeJoinedEvent event = new ChallengeJoinedEvent(
                "user-2",
                "challenge-1",
                "creator-1",
                "30 Day Fitness");

        ArgumentCaptor<Map<String, Object>> payloadCaptor = mapCaptor();

        listener.onChallengeJoined(event);

        verify(notificationDispatchService, times(1)).createNotification(
                eq("creator-1"),
                eq("NEW_PARTICIPANT"),
                eq("challenge-1:user-2"),
                payloadCaptor.capture());

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload)
                .containsEntry("title", "Có người tham gia thử thách")
                .containsEntry("challengeId", "challenge-1")
                .containsEntry("participantUserId", "user-2")
                .containsEntry("challengeTitle", "30 Day Fitness")
                .containsKeys("message", "eventId", "occurredAt");
    }

    @Test
    void challengeJoinedEventShouldSkipWhenParticipantIsCreator() {
        ChallengeJoinedEvent event = new ChallengeJoinedEvent(
                "creator-1",
                "challenge-1",
                "creator-1",
                "30 Day Fitness");

        listener.onChallengeJoined(event);

        verifyNoInteractions(notificationDispatchService);
    }

    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        Class<Map<String, Object>> mapClass = (Class<Map<String, Object>>) (Class<?>) Map.class;
        return ArgumentCaptor.forClass(mapClass);
    }
}
