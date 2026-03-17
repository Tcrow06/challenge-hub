package com.challengehub.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.challengehub.service.SubmissionApprovedStreakService;

@Component
public class SubmissionApprovedStreakEventListener {

    private final SubmissionApprovedStreakService submissionApprovedStreakService;

    public SubmissionApprovedStreakEventListener(SubmissionApprovedStreakService submissionApprovedStreakService) {
        this.submissionApprovedStreakService = submissionApprovedStreakService;
    }

    @EventListener
    public void onSubmissionApproved(SubmissionApprovedEvent event) {
        submissionApprovedStreakService.updateStreak(event);
    }
}
