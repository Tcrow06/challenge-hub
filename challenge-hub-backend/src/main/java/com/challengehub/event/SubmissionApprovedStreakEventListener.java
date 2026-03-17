package com.challengehub.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.challengehub.service.SubmissionApprovedStreakService;

@Component
public class SubmissionApprovedStreakEventListener {

    private final SubmissionApprovedStreakService submissionApprovedStreakService;

    public SubmissionApprovedStreakEventListener(SubmissionApprovedStreakService submissionApprovedStreakService) {
        this.submissionApprovedStreakService = submissionApprovedStreakService;
    }

    @Async("domainEventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionApproved(SubmissionApprovedEvent event) {
        submissionApprovedStreakService.updateStreak(event);
    }
}
