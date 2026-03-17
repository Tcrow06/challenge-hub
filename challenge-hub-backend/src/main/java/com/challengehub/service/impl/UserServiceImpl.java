package com.challengehub.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.challengehub.dto.request.UserUpdateRequest;
import com.challengehub.dto.response.UserProfileResponse;
import com.challengehub.dto.response.UserStatsResponse;
import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.UserBadgeEntity;
import com.challengehub.entity.postgres.UserEntity;
import com.challengehub.exception.ApiException;
import com.challengehub.exception.ErrorCode;
import com.challengehub.repository.postgres.SubmissionRepository;
import com.challengehub.repository.postgres.UserBadgeRepository;
import com.challengehub.repository.postgres.UserChallengeRepository;
import com.challengehub.repository.postgres.UserRepository;
import com.challengehub.service.UserService;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final SubmissionRepository submissionRepository;
    private final UserBadgeRepository userBadgeRepository;

    public UserServiceImpl(UserRepository userRepository,
            UserChallengeRepository userChallengeRepository,
            SubmissionRepository submissionRepository,
            UserBadgeRepository userBadgeRepository) {
        this.userRepository = userRepository;
        this.userChallengeRepository = userChallengeRepository;
        this.submissionRepository = submissionRepository;
        this.userBadgeRepository = userBadgeRepository;
    }

    @Override
    public UserProfileResponse getMyProfile(String currentUserId) {
        UUID userId = parseUuid(currentUserId, "currentUserId");
        UserEntity user = findUser(userId);
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(String currentUserId, UserUpdateRequest request) {
        UUID userId = parseUuid(currentUserId, "currentUserId");
        UserEntity user = findUser(userId);

        user.setDisplayName(trimToNull(request.displayName()));
        user.setBio(trimToNull(request.bio()));
        user.setAvatarUrl(trimToNull(request.avatarUrl()));

        UserEntity savedUser = userRepository.save(user);
        return toProfileResponse(savedUser);
    }

    @Override
    public UserStatsResponse getUserStats(String userId) {
        UUID targetUserId = parseUuid(userId, "id");
        UserEntity user = findUser(targetUserId);

        long challengesJoined = userChallengeRepository.countByUser_Id(targetUserId);
        long challengesCompleted = userChallengeRepository
                .countByUser_IdAndStatus(targetUserId, Enums.UserChallengeStatus.DONE);
        long submissionsCount = submissionRepository.countByUser_Id(targetUserId);
        int currentStreak = user.getStreakCount() == null ? 0 : user.getStreakCount();

        List<UserStatsResponse.BadgeView> badges = userBadgeRepository
                .findByUser_IdOrderByEarnedAtDesc(targetUserId)
                .stream()
                .map(this::toBadgeView)
                .toList();

        return new UserStatsResponse(
                challengesJoined,
                challengesCompleted,
                submissionsCount,
                currentStreak,
                badges);
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, fieldName + " khong hop le");
        }
    }

    private UserEntity findUser(UUID userId) {
        return userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Khong tim thay nguoi dung"));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserProfileResponse toProfileResponse(UserEntity user) {
        return new UserProfileResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getCreatedAt());
    }

    private UserStatsResponse.BadgeView toBadgeView(UserBadgeEntity userBadge) {
        return new UserStatsResponse.BadgeView(
                userBadge.getBadge().getCode(),
                userBadge.getBadge().getName(),
                userBadge.getBadge().getIconUrl(),
                userBadge.getEarnedAt());
    }
}
