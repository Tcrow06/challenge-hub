package com.challengehub.service.impl;

import com.challengehub.dto.request.CreateChallengeRequest;
import com.challengehub.dto.request.CreateTaskRequest;
import com.challengehub.dto.request.UpdateChallengeStatusRequest;
import com.challengehub.dto.response.ChallengeResponse;
import com.challengehub.dto.response.TaskResponse;
import com.challengehub.entity.postgres.ChallengeEntity;
import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.TaskEntity;
import com.challengehub.entity.postgres.UserChallengeEntity;
import com.challengehub.entity.postgres.UserEntity;
import com.challengehub.exception.ApiException;
import com.challengehub.repository.postgres.ChallengeRepository;
import com.challengehub.repository.postgres.TaskRepository;
import com.challengehub.repository.postgres.UserChallengeRepository;
import com.challengehub.repository.postgres.UserRepository;
import com.challengehub.service.ChallengeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final UserChallengeRepository userChallengeRepository;

    public ChallengeServiceImpl(ChallengeRepository challengeRepository,
                                TaskRepository taskRepository,
                                UserRepository userRepository,
                                UserChallengeRepository userChallengeRepository) {
        this.challengeRepository = challengeRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.userChallengeRepository = userChallengeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeResponse> listChallenges() {
        return challengeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(c -> toResponse(c, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChallengeResponse getChallengeById(String challengeId, String currentUserId) {
        ChallengeEntity challenge = findChallenge(challengeId);
        return toResponse(challenge, currentUserId);
    }

    @Override
    public ChallengeResponse createChallenge(CreateChallengeRequest request, String currentUserId, String role) {
        ensureRole(role, "CREATOR", "ADMIN");
        UserEntity creator = findUser(currentUserId);

        ChallengeEntity challenge = new ChallengeEntity();
        challenge.setCreator(creator);
        challenge.setTitle(request.title().trim());
        challenge.setDescription(request.description());
        challenge.setDifficulty(request.difficulty());
        challenge.setCoverUrl(request.coverUrl());
        challenge.setStartDate(request.startDate());
        challenge.setEndDate(request.endDate());
        challenge.setMaxParticipants(request.maxParticipants());
        challenge.setAllowLateJoin(request.allowLateJoin() == null ? Boolean.TRUE : request.allowLateJoin());
        challenge.setTaskUnlockMode(request.taskUnlockMode());
        challenge.setStatus(Enums.ChallengeStatus.DRAFT);

        challenge = challengeRepository.save(challenge);
        return toResponse(challenge, currentUserId);
    }

    @Override
    public ChallengeResponse updateChallengeStatus(String challengeId,
                                                   UpdateChallengeStatusRequest request,
                                                   String currentUserId,
                                                   String role) {
        ChallengeEntity challenge = findChallenge(challengeId);
        boolean isOwner = challenge.getCreator().getId().toString().equals(currentUserId);
        if (!isOwner && !"ADMIN".equals(role)) {
            throw new ApiException(com.challengehub.exception.ErrorCode.FORBIDDEN, "Ban khong co quyen thay doi challenge nay");
        }

        Enums.ChallengeStatus from = challenge.getStatus();
        Enums.ChallengeStatus to = request.status();

        if (from == Enums.ChallengeStatus.DRAFT && to == Enums.ChallengeStatus.PUBLISHED) {
            validatePublishPreconditions(challenge);
            challenge.setStatus(to);
        } else if (from == Enums.ChallengeStatus.PUBLISHED && to == Enums.ChallengeStatus.DRAFT) {
            if (userChallengeRepository.existsByChallenge_Id(challenge.getId())) {
                throw new ApiException(com.challengehub.exception.ErrorCode.CHALLENGE_HAS_PARTICIPANTS, "Khong the thu hoi challenge da co nguoi tham gia");
            }
            challenge.setStatus(to);
        } else if (from == Enums.ChallengeStatus.ENDED && to == Enums.ChallengeStatus.ARCHIVED) {
            challenge.setStatus(to);
        } else {
            throw new ApiException(com.challengehub.exception.ErrorCode.CHALLENGE_INVALID_TRANSITION, "Chuyen trang thai challenge khong hop le");
        }

        challenge = challengeRepository.save(challenge);
        return toResponse(challenge, currentUserId);
    }

    @Override
    public void joinChallenge(String challengeId, String currentUserId) {
        ChallengeEntity challenge = findChallenge(challengeId);
        UUID userId = UUID.fromString(currentUserId);

        boolean joinable = challenge.getStatus() == Enums.ChallengeStatus.PUBLISHED
                || (challenge.getStatus() == Enums.ChallengeStatus.ONGOING && Boolean.TRUE.equals(challenge.getAllowLateJoin()));
        if (!joinable) {
            throw new ApiException(com.challengehub.exception.ErrorCode.CHALLENGE_NOT_JOINABLE, "Challenge hien tai khong cho phep tham gia");
        }

        if (userChallengeRepository.existsByUser_IdAndChallenge_Id(userId, challenge.getId())) {
            throw new ApiException(com.challengehub.exception.ErrorCode.CHALLENGE_ALREADY_JOINED, "Ban da tham gia challenge nay");
        }

        if (challenge.getMaxParticipants() != null) {
            long activeCount = userChallengeRepository.countByChallenge_IdAndStatus(challenge.getId(), Enums.UserChallengeStatus.ACTIVE);
            if (activeCount >= challenge.getMaxParticipants()) {
                throw new ApiException(com.challengehub.exception.ErrorCode.CHALLENGE_FULL, "Challenge da du so luong nguoi tham gia");
            }
        }

        UserChallengeEntity userChallenge = new UserChallengeEntity();
        userChallenge.setUser(findUser(currentUserId));
        userChallenge.setChallenge(challenge);
        userChallenge.setStatus(Enums.UserChallengeStatus.ACTIVE);
        userChallenge.setTotalScore(0);
        userChallengeRepository.save(userChallenge);
    }

    @Override
    public void quitChallenge(String challengeId, String currentUserId) {
        ChallengeEntity challenge = findChallenge(challengeId);
        UUID userId = UUID.fromString(currentUserId);

        UserChallengeEntity userChallenge = userChallengeRepository.findByUser_IdAndChallenge_Id(userId, challenge.getId())
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.CHALLENGE_NOT_JOINED, "Ban chua tham gia challenge nay"));

        if (userChallenge.getStatus() == Enums.UserChallengeStatus.QUIT) {
            throw new ApiException(com.challengehub.exception.ErrorCode.CHALLENGE_ALREADY_QUIT, "Ban da roi khoi challenge nay");
        }

        userChallenge.setStatus(Enums.UserChallengeStatus.QUIT);
        userChallengeRepository.save(userChallenge);
    }

    @Override
    public TaskResponse createTask(String challengeId, CreateTaskRequest request, String currentUserId, String role) {
        ChallengeEntity challenge = findChallenge(challengeId);
        boolean isOwner = challenge.getCreator().getId().toString().equals(currentUserId);
        if (!isOwner && !"ADMIN".equals(role)) {
            throw new ApiException(com.challengehub.exception.ErrorCode.FORBIDDEN, "Ban khong co quyen tao task cho challenge nay");
        }
        if (challenge.getStatus() != Enums.ChallengeStatus.DRAFT) {
            throw new ApiException(com.challengehub.exception.ErrorCode.CHALLENGE_INVALID_TRANSITION, "Chi co the tao task khi challenge o trang thai DRAFT");
        }
        if (taskRepository.findByChallenge_IdAndDayNumber(challenge.getId(), request.dayNumber()).isPresent()) {
            throw new ApiException(com.challengehub.exception.ErrorCode.VALIDATION_FAILED, "Day number da ton tai trong challenge");
        }

        TaskEntity task = new TaskEntity();
        task.setChallenge(challenge);
        task.setTitle(request.title().trim());
        task.setContent(request.content());
        task.setDayNumber(request.dayNumber());
        task.setMaxScore(request.maxScore());
        task = taskRepository.save(task);

        return toTaskResponse(task, isUnlocked(challenge, task));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(String challengeId, String currentUserId) {
        ChallengeEntity challenge = findChallenge(challengeId);
        return taskRepository.findByChallenge_Id(challenge.getId())
                .stream()
                .sorted((a, b) -> Integer.compare(a.getDayNumber(), b.getDayNumber()))
                .map(task -> toTaskResponse(task, isUnlocked(challenge, task)))
                .toList();
    }

    private ChallengeEntity findChallenge(String challengeId) {
        return challengeRepository.findById(UUID.fromString(challengeId))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.CHALLENGE_NOT_FOUND, "Khong tim thay challenge"));
    }

    private UserEntity findUser(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.NOT_FOUND, "Khong tim thay nguoi dung"));
    }

    private void ensureRole(String role, String... allowedRoles) {
        for (String allowedRole : allowedRoles) {
            if (allowedRole.equals(role)) {
                return;
            }
        }
        throw new ApiException(com.challengehub.exception.ErrorCode.FORBIDDEN, "Ban khong co quyen thuc hien hanh dong nay");
    }

    private void validatePublishPreconditions(ChallengeEntity challenge) {
        long taskCount = taskRepository.findByChallenge_Id(challenge.getId()).size();
        if (taskCount < 1) {
            throw new ApiException(com.challengehub.exception.ErrorCode.CHALLENGE_MISSING_TASKS, "Challenge can it nhat 1 task de publish");
        }
        if (challenge.getStartDate() == null || challenge.getEndDate() == null || !challenge.getStartDate().isBefore(challenge.getEndDate())) {
            throw new ApiException(com.challengehub.exception.ErrorCode.CHALLENGE_MISSING_DATES, "Challenge can start_date va end_date hop le");
        }
    }

    private boolean isUnlocked(ChallengeEntity challenge, TaskEntity task) {
        if (challenge.getStatus() == Enums.ChallengeStatus.DRAFT || challenge.getStatus() == Enums.ChallengeStatus.PUBLISHED) {
            return false;
        }
        if (challenge.getTaskUnlockMode() == Enums.TaskUnlockMode.ALL_AT_ONCE) {
            return true;
        }
        if (challenge.getStartDate() == null) {
            return false;
        }
        Instant unlockAt = challenge.getStartDate().plus(task.getDayNumber() - 1L, ChronoUnit.DAYS);
        return !Instant.now().isBefore(unlockAt);
    }

    private ChallengeResponse toResponse(ChallengeEntity challenge, String currentUserId) {
        int taskCount = taskRepository.findByChallenge_Id(challenge.getId()).size();
        long participantCount = userChallengeRepository.countByChallenge_IdAndStatus(challenge.getId(), Enums.UserChallengeStatus.ACTIVE);
        Boolean isJoined = null;
        if (currentUserId != null) {
            isJoined = userChallengeRepository.existsByUser_IdAndChallenge_Id(UUID.fromString(currentUserId), challenge.getId());
        }
        return new ChallengeResponse(
                challenge.getId().toString(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getStatus(),
                challenge.getDifficulty(),
                challenge.getCoverUrl(),
                challenge.getStartDate(),
                challenge.getEndDate(),
                challenge.getMaxParticipants(),
                challenge.getAllowLateJoin(),
                challenge.getTaskUnlockMode(),
                taskCount,
                participantCount,
                new ChallengeResponse.CreatorView(
                        challenge.getCreator().getId().toString(),
                        challenge.getCreator().getUsername(),
                        challenge.getCreator().getAvatarUrl()
                ),
                isJoined,
                challenge.getCreatedAt(),
                challenge.getUpdatedAt()
        );
    }

    private TaskResponse toTaskResponse(TaskEntity task, boolean unlocked) {
        return new TaskResponse(
                task.getId().toString(),
                task.getDayNumber(),
                task.getTitle(),
                task.getContent(),
                task.getMaxScore(),
                unlocked
        );
    }
}
