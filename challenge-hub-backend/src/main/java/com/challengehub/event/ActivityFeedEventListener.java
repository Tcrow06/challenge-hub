package com.challengehub.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.challengehub.service.ActivityFeedService;

@Component
public class ActivityFeedEventListener {

    private static final String TYPE_JOIN_CHALLENGE = "JOIN_CHALLENGE";
    private static final String TYPE_COMPLETE_TASK = "COMPLETE_TASK";

    private final ActivityFeedService activityFeedService;

    public ActivityFeedEventListener(ActivityFeedService activityFeedService) {
        this.activityFeedService = activityFeedService;
    }

    @EventListener
    public void onChallengeJoined(ChallengeJoinedEvent event) {
        activityFeedService.createFeed(
                event.getUserId(),
                TYPE_JOIN_CHALLENGE,
                event.getChallengeId());
    }

    @EventListener
    public void onSubmissionApproved(SubmissionApprovedEvent event) {
        activityFeedService.createFeed(
                event.getUserId(),
                TYPE_COMPLETE_TASK,
                event.getSubmissionId());
    }
}