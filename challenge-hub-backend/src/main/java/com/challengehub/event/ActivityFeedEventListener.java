package com.challengehub.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.challengehub.service.ActivityFeedService;

@Component
public class ActivityFeedEventListener {

    private static final String TYPE_JOIN_CHALLENGE = "JOIN_CHALLENGE";
    private static final String TYPE_COMPLETE_TASK = "COMPLETE_TASK";

    private final ActivityFeedService activityFeedService;

    public ActivityFeedEventListener(ActivityFeedService activityFeedService) {
        this.activityFeedService = activityFeedService;
    }

    @Async("domainEventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChallengeJoined(ChallengeJoinedEvent event) {
        activityFeedService.createFeed(
                event.getUserId(),
                TYPE_JOIN_CHALLENGE,
                event.getChallengeId());
    }

    @Async("domainEventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionApproved(SubmissionApprovedEvent event) {
        activityFeedService.createFeed(
                event.getUserId(),
                TYPE_COMPLETE_TASK,
                event.getSubmissionId());
    }
}