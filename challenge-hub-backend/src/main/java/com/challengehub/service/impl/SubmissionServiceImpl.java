package com.challengehub.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.challengehub.dto.request.CreateSubmissionRequest;
import com.challengehub.dto.request.UpdateSubmissionStatusRequest;
import com.challengehub.dto.response.SubmissionCreateResponse;
import com.challengehub.dto.response.SubmissionResponse;
import com.challengehub.entity.postgres.ChallengeEntity;
import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.MediaEntity;
import com.challengehub.entity.postgres.SubmissionEntity;
import com.challengehub.entity.postgres.TaskEntity;
import com.challengehub.entity.postgres.UserChallengeEntity;
import com.challengehub.entity.postgres.UserEntity;
import com.challengehub.event.EventPublisher;
import com.challengehub.event.SubmissionApprovedEvent;
import com.challengehub.event.SubmissionCreatedEvent;
import com.challengehub.exception.ApiException;
import com.challengehub.repository.postgres.MediaRepository;
import com.challengehub.repository.postgres.SubmissionRepository;
import com.challengehub.repository.postgres.TaskRepository;
import com.challengehub.repository.postgres.UserChallengeRepository;
import com.challengehub.repository.postgres.UserRepository;
import com.challengehub.service.SubmissionService;

@Service
@Transactional
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final EventPublisher eventPublisher;

    public SubmissionServiceImpl(SubmissionRepository submissionRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            MediaRepository mediaRepository,
            UserChallengeRepository userChallengeRepository,
            EventPublisher eventPublisher) {
        this.submissionRepository = submissionRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.mediaRepository = mediaRepository;
        this.userChallengeRepository = userChallengeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SubmissionCreateResponse submitTask(String taskId, CreateSubmissionRequest request, String currentUserId) {
        TaskEntity task = findTask(taskId);
        UUID userId = UUID.fromString(currentUserId);
        ChallengeEntity challenge = task.getChallenge();

        UserChallengeEntity membership = userChallengeRepository
                .findByUser_IdAndChallenge_IdAndStatus(userId, challenge.getId(), Enums.UserChallengeStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.SUBMISSION_NOT_PARTICIPANT,
                        "Ban chua tham gia challenge nay"));

        if (challenge.getStatus() != Enums.ChallengeStatus.ONGOING) {
            throw new ApiException(com.challengehub.exception.ErrorCode.SUBMISSION_CHALLENGE_ENDED,
                    "Challenge da ket thuc hoac chua bat dau");
        }
        if (!isUnlocked(challenge, task)) {
            throw new ApiException(com.challengehub.exception.ErrorCode.TASK_NOT_UNLOCKED, "Task chua duoc mo");
        }
        if (submissionRepository.findByTask_IdAndUser_Id(task.getId(), userId).isPresent()) {
            throw new ApiException(com.challengehub.exception.ErrorCode.SUBMISSION_ALREADY_EXISTS,
                    "Ban da nop bai cho task nay");
        }

        SubmissionEntity submission = new SubmissionEntity();
        submission.setTask(task);
        submission.setUser(membership.getUser());
        submission.setDescription(request.description());
        submission.setMedia(resolveMedia(request.mediaId(), currentUserId));
        submission.setStatus(Enums.SubmissionStatus.PENDING);
        submission.setSubmittedAt(Instant.now());

        submission = submissionRepository.save(submission);

        SubmissionCreatedEvent submissionCreatedEvent = new SubmissionCreatedEvent(
                currentUserId,
                submission.getId().toString(),
                challenge.getId().toString(),
                task.getId().toString(),
                request.mediaId());
        publishAfterCommit(submissionCreatedEvent);

        return toCreateResponse(submission);
    }

    @Override
    public SubmissionResponse resubmit(String submissionId, CreateSubmissionRequest request, String currentUserId) {
        SubmissionEntity submission = findSubmission(submissionId);
        if (!submission.getUser().getId().toString().equals(currentUserId)) {
            throw new ApiException(com.challengehub.exception.ErrorCode.FORBIDDEN,
                    "Ban khong co quyen sua bai nop nay");
        }

        ChallengeEntity challenge = submission.getTask().getChallenge();
        if (challenge.getStatus() != Enums.ChallengeStatus.ONGOING) {
            throw new ApiException(com.challengehub.exception.ErrorCode.SUBMISSION_CHALLENGE_ENDED,
                    "Challenge da ket thuc");
        }
        if (submission.getStatus() == Enums.SubmissionStatus.APPROVED) {
            throw new ApiException(com.challengehub.exception.ErrorCode.SUBMISSION_ALREADY_APPROVED,
                    "Khong the sua bai da duoc duyet");
        }
        if (submission.getStatus() != Enums.SubmissionStatus.REJECTED) {
            throw new ApiException(com.challengehub.exception.ErrorCode.SUBMISSION_INVALID_RESUBMIT,
                    "Chi duoc nop lai khi bai o trang thai REJECTED");
        }

        submission.setDescription(request.description());
        submission.setMedia(resolveMedia(request.mediaId(), currentUserId));
        submission.setStatus(Enums.SubmissionStatus.PENDING);
        submission.setScore(null);
        submission.setReviewer(null);
        submission.setReviewedAt(null);
        submission.setRejectReason(null);

        submission = submissionRepository.save(submission);
        return toResponse(submission);
    }

    @Override
    public SubmissionResponse updateSubmissionStatus(String submissionId,
            UpdateSubmissionStatusRequest request,
            String reviewerId,
            String reviewerRole) {
        if (!"MODERATOR".equals(reviewerRole) && !"ADMIN".equals(reviewerRole)) {
            throw new ApiException(com.challengehub.exception.ErrorCode.FORBIDDEN, "Ban khong co quyen duyet bai");
        }

        SubmissionEntity submission = findSubmissionForUpdate(submissionId);
        TaskEntity task = submission.getTask();
        Enums.SubmissionStatus currentStatus = submission.getStatus();
        Enums.SubmissionStatus targetStatus = request.status();

        ensureValidStatusTransition(currentStatus, targetStatus);

        if (currentStatus == targetStatus) {
            return toResponse(submission);
        }

        if (targetStatus == Enums.SubmissionStatus.APPROVED) {
            if (request.score() == null) {
                throw new ApiException(com.challengehub.exception.ErrorCode.VALIDATION_FAILED,
                        "Score la bat buoc khi duyet bai");
            }
            if (request.score() > task.getMaxScore()) {
                throw new ApiException(com.challengehub.exception.ErrorCode.SUBMISSION_SCORE_EXCEEDED,
                        "Score vuot qua max_score cua task");
            }
            submission.setStatus(Enums.SubmissionStatus.APPROVED);
            submission.setScore(request.score());
            submission.setRejectReason(null);
        } else if (targetStatus == Enums.SubmissionStatus.REJECTED) {
            if (request.rejectReason() == null || request.rejectReason().isBlank()) {
                throw new ApiException(com.challengehub.exception.ErrorCode.VALIDATION_FAILED,
                        "rejectReason la bat buoc khi tu choi bai");
            }
            submission.setStatus(Enums.SubmissionStatus.REJECTED);
            submission.setScore(null);
            submission.setRejectReason(request.rejectReason().trim());
        } else {
            throw new ApiException(com.challengehub.exception.ErrorCode.VALIDATION_FAILED,
                    "Chi cho phep cap nhat sang APPROVED hoac REJECTED");
        }

        submission.setReviewer(findUser(reviewerId));
        submission.setReviewedAt(Instant.now());
        submission = submissionRepository.save(submission);

        if (submission.getStatus() == Enums.SubmissionStatus.APPROVED && submission.getScore() != null) {
            SubmissionApprovedEvent submissionApprovedEvent = new SubmissionApprovedEvent(
                    submission.getUser().getId().toString(),
                    submission.getId().toString(),
                    submission.getTask().getChallenge().getId().toString(),
                    submission.getTask().getId().toString(),
                    reviewerId,
                    submission.getScore(),
                    submission.getTask().getMaxScore());
            publishAfterCommit(submissionApprovedEvent);
        }

        return toResponse(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionById(String submissionId, String currentUserId, String currentUserRole) {
        SubmissionEntity submission = findSubmission(submissionId);
        boolean isOwner = submission.getUser().getId().toString().equals(currentUserId);
        boolean privileged = "MODERATOR".equals(currentUserRole) || "ADMIN".equals(currentUserRole);
        if (!isOwner && !privileged) {
            throw new ApiException(com.challengehub.exception.ErrorCode.FORBIDDEN,
                    "Ban khong co quyen xem bai nop nay");
        }
        return toResponse(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SubmissionResponse> getMySubmissions(String currentUserId, String challengeId, String status,
            int page, int size) {
        UUID userId = UUID.fromString(currentUserId);
        Pageable pageable = PageRequest.of(normalizePage(page) - 1, normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<SubmissionEntity> submissions;
        if (challengeId != null && status != null) {
            submissions = submissionRepository.findByUser_IdAndTask_Challenge_IdAndStatus(
                    userId,
                    UUID.fromString(challengeId),
                    parseSubmissionStatus(status),
                    pageable);
        } else if (challengeId != null) {
            submissions = submissionRepository.findByUser_IdAndTask_Challenge_Id(userId, UUID.fromString(challengeId),
                    pageable);
        } else if (status != null) {
            submissions = submissionRepository.findByUser_IdAndStatus(userId, parseSubmissionStatus(status), pageable);
        } else {
            submissions = submissionRepository.findByUser_Id(userId, pageable);
        }
        return toPageResult(submissions.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SubmissionResponse> getPendingSubmissions(String currentUserRole, String challengeId, int page,
            int size) {
        if (!"MODERATOR".equals(currentUserRole) && !"ADMIN".equals(currentUserRole)) {
            throw new ApiException(com.challengehub.exception.ErrorCode.FORBIDDEN,
                    "Ban khong co quyen xem danh sach cho duyet");
        }

        Pageable pageable = PageRequest.of(normalizePage(page) - 1, normalizeSize(size),
                Sort.by(Sort.Direction.ASC, "submittedAt"));
        Page<SubmissionEntity> submissions = challengeId == null
                ? submissionRepository.findByStatus(Enums.SubmissionStatus.PENDING, pageable)
                : submissionRepository.findByStatusAndTask_Challenge_Id(Enums.SubmissionStatus.PENDING,
                        UUID.fromString(challengeId), pageable);

        return toPageResult(submissions.map(this::toResponse));
    }

    private int normalizePage(int page) {
        return page < 1 ? 1 : page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }

    private PageResult<SubmissionResponse> toPageResult(Page<SubmissionResponse> page) {
        return new PageResult<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private void ensureValidStatusTransition(Enums.SubmissionStatus current, Enums.SubmissionStatus target) {
        if (current == target) {
            return;
        }

        if (current == Enums.SubmissionStatus.PENDING
                && (target == Enums.SubmissionStatus.APPROVED || target == Enums.SubmissionStatus.REJECTED)) {
            return;
        }

        throw new ApiException(com.challengehub.exception.ErrorCode.VALIDATION_FAILED,
                "Submission status transition khong hop le");
    }

    private SubmissionEntity findSubmission(String submissionId) {
        return submissionRepository.findById(Objects.requireNonNull(UUID.fromString(submissionId)))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.SUBMISSION_NOT_FOUND,
                        "Khong tim thay bai nop"));
    }

    private SubmissionEntity findSubmissionForUpdate(String submissionId) {
        return submissionRepository.findByIdForUpdate(Objects.requireNonNull(UUID.fromString(submissionId)))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.SUBMISSION_NOT_FOUND,
                        "Khong tim thay bai nop"));
    }

    private TaskEntity findTask(String taskId) {
        return taskRepository.findById(Objects.requireNonNull(UUID.fromString(taskId)))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.TASK_NOT_FOUND,
                        "Khong tim thay task"));
    }

    private UserEntity findUser(String userId) {
        return userRepository.findById(Objects.requireNonNull(UUID.fromString(userId)))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.NOT_FOUND,
                        "Khong tim thay nguoi dung"));
    }

    private MediaEntity resolveMedia(String mediaId, String currentUserId) {
        if (mediaId == null || mediaId.isBlank()) {
            return null;
        }
        MediaEntity media = mediaRepository.findById(Objects.requireNonNull(UUID.fromString(mediaId)))
                .orElseThrow(
                        () -> new ApiException(com.challengehub.exception.ErrorCode.NOT_FOUND, "Khong tim thay media"));
        if (!media.getUser().getId().toString().equals(currentUserId)) {
            throw new ApiException(com.challengehub.exception.ErrorCode.FORBIDDEN,
                    "Ban khong co quyen su dung media nay");
        }
        if (media.getStatus() != Enums.MediaStatus.CONFIRMED) {
            throw new ApiException(com.challengehub.exception.ErrorCode.VALIDATION_FAILED,
                    "Media chua duoc xac nhan upload");
        }
        return media;
    }

    private Enums.SubmissionStatus parseSubmissionStatus(String status) {
        try {
            return Enums.SubmissionStatus.valueOf(status.toUpperCase());
        } catch (Exception ex) {
            throw new ApiException(com.challengehub.exception.ErrorCode.VALIDATION_FAILED,
                    "Gia tri status khong hop le");
        }
    }

    private boolean isUnlocked(ChallengeEntity challenge, TaskEntity task) {
        if (challenge.getTaskUnlockMode() == Enums.TaskUnlockMode.ALL_AT_ONCE) {
            return true;
        }
        if (challenge.getStartDate() == null) {
            return false;
        }
        Instant unlockAt = challenge.getStartDate().plus(task.getDayNumber() - 1L, ChronoUnit.DAYS);
        return !Instant.now().isBefore(unlockAt);
    }

    private SubmissionResponse toResponse(SubmissionEntity submission) {
        MediaEntity media = submission.getMedia();
        return new SubmissionResponse(
                submission.getId().toString(),
                new SubmissionResponse.TaskView(
                        submission.getTask().getId().toString(),
                        submission.getTask().getTitle(),
                        submission.getTask().getDayNumber(),
                        submission.getTask().getMaxScore()),
                new SubmissionResponse.ChallengeView(
                        submission.getTask().getChallenge().getId().toString(),
                        submission.getTask().getChallenge().getTitle()),
                submission.getDescription(),
                media == null ? null
                        : new SubmissionResponse.MediaView(
                                media.getId().toString(),
                                media.getFileUrl(),
                                media.getFileType()),
                submission.getStatus(),
                submission.getScore(),
                submission.getRejectReason(),
                submission.getSubmittedAt(),
                submission.getReviewedAt());
    }

    private SubmissionCreateResponse toCreateResponse(SubmissionEntity submission) {
        Instant createdAt = submission.getCreatedAt() != null ? submission.getCreatedAt() : submission.getSubmittedAt();
        return new SubmissionCreateResponse(
                submission.getId().toString(),
                submission.getTask().getId().toString(),
                submission.getStatus(),
                createdAt);
    }

    private void publishAfterCommit(com.challengehub.event.DomainEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publish(event);
                }
            });
            return;
        }
        eventPublisher.publish(event);
    }
}