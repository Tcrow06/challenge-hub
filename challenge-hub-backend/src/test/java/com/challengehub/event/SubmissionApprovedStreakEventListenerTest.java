package com.challengehub.event;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.challengehub.service.SubmissionApprovedStreakService;

@ExtendWith(MockitoExtension.class)
class SubmissionApprovedStreakEventListenerTest {

    @Mock
    private SubmissionApprovedStreakService submissionApprovedStreakService;

    @InjectMocks
    private SubmissionApprovedStreakEventListener listener;

    @Test
    void submissionApprovedEventShouldTriggerStreakSideEffect() {
        SubmissionApprovedEvent event = new SubmissionApprovedEvent(
                "user-1",
                "submission-1",
                "challenge-1",
                "task-1",
                "reviewer-1",
                8,
                10);

        listener.onSubmissionApproved(event);

        verify(submissionApprovedStreakService, times(1)).updateStreak(event);
    }
}
