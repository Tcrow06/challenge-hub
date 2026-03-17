package com.challengehub.event;

import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.challengehub.service.NotificationDispatchService;

@Component
public class NotificationEventListener {

    private static final String TYPE_SUBMISSION_APPROVED = "SUBMISSION_APPROVED";
    private static final String TYPE_NEW_PARTICIPANT = "NEW_PARTICIPANT";

    private final NotificationDispatchService notificationDispatchService;

    public NotificationEventListener(NotificationDispatchService notificationDispatchService) {
        this.notificationDispatchService = notificationDispatchService;
    }

    @EventListener
    public void onSubmissionApproved(SubmissionApprovedEvent event) {
        notificationDispatchService.createNotification(
                event.getUserId(),
                TYPE_SUBMISSION_APPROVED,
                event.getSubmissionId(),
                Map.of(
                        "title", "Bài nộp đã được duyệt",
                        "message",
                        String.format("Bài nộp của bạn đạt %d/%d điểm", event.getScore(), event.getMaxScore()),
                        "metadata", Map.of(
                                "submission_id", event.getSubmissionId(),
                                "challenge_id", event.getChallengeId(),
                                "task_id", event.getTaskId(),
                                "reviewer_id", event.getReviewerId(),
                                "score", event.getScore(),
                                "max_score", event.getMaxScore())));
    }

    @EventListener
    public void onChallengeJoined(ChallengeJoinedEvent event) {
        if (event.getUserId().equals(event.getChallengeCreatorId())) {
            return;
        }

        notificationDispatchService.createNotification(
                event.getChallengeCreatorId(),
                TYPE_NEW_PARTICIPANT,
                event.getChallengeId() + ":" + event.getUserId(),
                Map.of(
                        "title", "Có người tham gia thử thách",
                        "message",
                        String.format("Người dùng mới vừa tham gia challenge \"%s\"", event.getChallengeTitle()),
                        "metadata", Map.of(
                                "challenge_id", event.getChallengeId(),
                                "participant_user_id", event.getUserId(),
                                "challenge_title", event.getChallengeTitle())));
    }
}
