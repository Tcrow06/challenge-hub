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
        assertThat(payload).containsKeys("title", "message", "metadata");
        assertThat(payload.get("metadata")).isInstanceOf(Map.class);

        Map<String, Object> metadata = (Map<String, Object>) payload.get("metadata");
        assertThat(metadata)
                .containsEntry("submission_id", "submission-1")
                .containsEntry("challenge_id", "challenge-1")
                .containsEntry("task_id", "task-1")
                .containsEntry("reviewer_id", "reviewer-1")
                .containsEntry("score", 8)
                .containsEntry("max_score", 10);
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
        assertThat(payload).containsKeys("title", "message", "metadata");
        assertThat(payload.get("metadata")).isInstanceOf(Map.class);

        Map<String, Object> metadata = (Map<String, Object>) payload.get("metadata");
        assertThat(metadata)
                .containsEntry("challenge_id", "challenge-1")
                .containsEntry("participant_user_id", "user-2")
                .containsEntry("challenge_title", "30 Day Fitness");
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
