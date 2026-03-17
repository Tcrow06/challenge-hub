package com.challengehub.event;

import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.challengehub.service.NotificationDispatchService;

@Component
public class NotificationEventListener {

    private static final String TYPE_SUBMISSION_APPROVED = "SUBMISSION_APPROVED";
    private static final String TYPE_NEW_PARTICIPANT = "NEW_PARTICIPANT";

    private final NotificationDispatchService notificationDispatchService;

    public NotificationEventListener(NotificationDispatchService notificationDispatchService) {
        this.notificationDispatchService = notificationDispatchService;
    }

    @Async("domainEventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionApproved(SubmissionApprovedEvent event) {
        notificationDispatchService.createNotification(
                event.getUserId(),
                TYPE_SUBMISSION_APPROVED,
                event.getSubmissionId(),
                Map.ofEntries(
                        Map.entry("title", "Bài nộp đã được duyệt"),
                        Map.entry(
                                "message",
                                String.format("Bài nộp của bạn đạt %d/%d điểm", event.getScore(), event.getMaxScore())),
                        Map.entry("submissionId", event.getSubmissionId()),
                        Map.entry("challengeId", event.getChallengeId()),
                        Map.entry("taskId", event.getTaskId()),
                        Map.entry("reviewerId", event.getReviewerId()),
                        Map.entry("score", event.getScore()),
                        Map.entry("maxScore", event.getMaxScore()),
                        Map.entry("eventId", event.getEventId().toString()),
                        Map.entry("occurredAt", event.getOccurredAt().toString())));
    }

    @Async("domainEventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChallengeJoined(ChallengeJoinedEvent event) {
        if (event.getUserId().equals(event.getChallengeCreatorId())) {
            return;
        }

        notificationDispatchService.createNotification(
                event.getChallengeCreatorId(),
                TYPE_NEW_PARTICIPANT,
                event.getChallengeId() + ":" + event.getUserId(),
                Map.ofEntries(
                        Map.entry("title", "Có người tham gia thử thách"),
                        Map.entry(
                                "message",
                                String.format("Người dùng mới vừa tham gia challenge \"%s\"",
                                        event.getChallengeTitle())),
                        Map.entry("challengeId", event.getChallengeId()),
                        Map.entry("participantUserId", event.getUserId()),
                        Map.entry("challengeTitle", event.getChallengeTitle()),
                        Map.entry("eventId", event.getEventId().toString()),
                        Map.entry("occurredAt", event.getOccurredAt().toString())));
    }
}
