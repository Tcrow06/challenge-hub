package com.challengehub.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.challengehub.service.SubmissionApprovedScoreService;

@Component
public class SubmissionApprovedScoreEventListener {

    private final SubmissionApprovedScoreService submissionApprovedScoreService;

    public SubmissionApprovedScoreEventListener(SubmissionApprovedScoreService submissionApprovedScoreService) {
        this.submissionApprovedScoreService = submissionApprovedScoreService;
    }

    @Async("domainEventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionApproved(SubmissionApprovedEvent event) {
        submissionApprovedScoreService.applyScore(event);
    }
}
