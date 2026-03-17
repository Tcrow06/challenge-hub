package com.challengehub.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.challengehub.service.SubmissionApprovedScoreService;

@Component
public class SubmissionApprovedScoreEventListener {

    private final SubmissionApprovedScoreService submissionApprovedScoreService;

    public SubmissionApprovedScoreEventListener(SubmissionApprovedScoreService submissionApprovedScoreService) {
        this.submissionApprovedScoreService = submissionApprovedScoreService;
    }

    @EventListener
    public void onSubmissionApproved(SubmissionApprovedEvent event) {
        submissionApprovedScoreService.applyScore(event);
    }
}
