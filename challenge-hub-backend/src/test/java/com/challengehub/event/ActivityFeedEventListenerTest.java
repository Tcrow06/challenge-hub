package com.challengehub.event;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.challengehub.service.ActivityFeedService;

@ExtendWith(MockitoExtension.class)
class ActivityFeedEventListenerTest {

    @Mock
    private ActivityFeedService activityFeedService;

    @InjectMocks
    private ActivityFeedEventListener listener;

    @Test
    void challengeJoinedEventShouldCreateFeed() {
        ChallengeJoinedEvent event = new ChallengeJoinedEvent(
                "user-1",
                "challenge-1",
                "creator-1",
                "Challenge title");

        listener.onChallengeJoined(event);

        verify(activityFeedService, times(1)).createFeed("user-1", "JOIN_CHALLENGE", "challenge-1");
    }

    @Test
    void submissionApprovedEventShouldCreateFeed() {
        SubmissionApprovedEvent event = new SubmissionApprovedEvent(
                "user-1",
                "submission-1",
                "challenge-1",
                "task-1",
                "reviewer-1",
                8,
                10);

        listener.onSubmissionApproved(event);

        verify(activityFeedService, times(1)).createFeed("user-1", "COMPLETE_TASK", "submission-1");
    }
}
