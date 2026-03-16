package com.challengehub.service.impl;

import com.challengehub.dto.response.ActivityFeedResponse;
import com.challengehub.dto.response.LeaderboardResponse;
import com.challengehub.entity.mongodb.ActivityFeedDocument;
import com.challengehub.entity.postgres.ChallengeEntity;
import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.SubmissionEntity;
import com.challengehub.entity.postgres.UserEntity;
import com.challengehub.exception.ApiException;
import com.challengehub.repository.mongodb.ActivityFeedRepository;
import com.challengehub.repository.postgres.ChallengeRepository;
import com.challengehub.repository.postgres.SubmissionRepository;
import com.challengehub.repository.postgres.UserRepository;
import com.challengehub.service.SocialService;
import com.challengehub.service.SubmissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SocialServiceImpl implements SocialService {

    private final StringRedisTemplate redisTemplate;
    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final ActivityFeedRepository activityFeedRepository;

    public SocialServiceImpl(StringRedisTemplate redisTemplate,
                             ChallengeRepository challengeRepository,
                             UserRepository userRepository,
                             SubmissionRepository submissionRepository,
                             ActivityFeedRepository activityFeedRepository) {
        this.redisTemplate = redisTemplate;
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.activityFeedRepository = activityFeedRepository;
    }

    @Override
    public LeaderboardResponse getLeaderboard(String challengeId, int top, String currentUserId) {
        ChallengeEntity challenge = challengeRepository.findById(UUID.fromString(challengeId))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.CHALLENGE_NOT_FOUND, "Khong tim thay challenge"));

        int normalizedTop = top <= 0 ? 50 : Math.min(top, 100);
        String key = "leaderboard:" + challenge.getId();

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, 0, normalizedTop - 1L);

        List<LeaderboardResponse.RankingItem> rankings = new ArrayList<>();
        if (tuples != null && !tuples.isEmpty()) {
            List<UUID> userIds = tuples.stream().map(ZSetOperations.TypedTuple::getValue)
                    .filter(v -> v != null && !v.isBlank())
                    .map(UUID::fromString)
                    .toList();
            Map<UUID, UserEntity> usersById = new HashMap<>();
            userRepository.findAllById(userIds).forEach(u -> usersById.put(u.getId(), u));

            Map<UUID, Long> approvedCountByUser = new HashMap<>();
            List<SubmissionEntity> approved = submissionRepository
                    .findByTask_Challenge_IdAndStatus(challenge.getId(), Enums.SubmissionStatus.APPROVED);
            for (SubmissionEntity submission : approved) {
                UUID uid = submission.getUser().getId();
                approvedCountByUser.put(uid, approvedCountByUser.getOrDefault(uid, 0L) + 1L);
            }

            long rank = 1;
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() == null) {
                    continue;
                }
                UUID uid = UUID.fromString(tuple.getValue());
                UserEntity user = usersById.get(uid);
                if (user == null) {
                    rank++;
                    continue;
                }
                long totalScore = tuple.getScore() == null ? 0L : tuple.getScore().longValue();
                long tasksCompleted = approvedCountByUser.getOrDefault(uid, 0L);
                rankings.add(new LeaderboardResponse.RankingItem(
                        rank,
                        new LeaderboardResponse.UserView(user.getId().toString(), user.getUsername(), user.getAvatarUrl()),
                        totalScore,
                        tasksCompleted
                ));
                rank++;
            }
        }

        LeaderboardResponse.MyRank myRank = null;
        if (currentUserId != null && !currentUserId.isBlank()) {
            Long rank = redisTemplate.opsForZSet().reverseRank(key, currentUserId);
            Double score = redisTemplate.opsForZSet().score(key, currentUserId);
            if (rank != null && score != null) {
                long tasksCompleted = submissionRepository.countByTask_Challenge_IdAndUser_IdAndStatus(
                        challenge.getId(),
                        UUID.fromString(currentUserId),
                        Enums.SubmissionStatus.APPROVED
                );
                myRank = new LeaderboardResponse.MyRank(rank + 1, score.longValue(), tasksCompleted);
            }
        }

        return new LeaderboardResponse(challenge.getId().toString(), rankings, myRank);
    }

    @Override
    public SubmissionService.PageResult<ActivityFeedResponse> getFeed(int page, int size) {
        Pageable pageable = PageRequest.of(normalizePage(page) - 1, normalizeSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ActivityFeedDocument> feedPage = activityFeedRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<UUID> userIds = feedPage.getContent().stream()
                .map(ActivityFeedDocument::getUserId)
                .filter(id -> id != null && !id.isBlank())
                .map(UUID::fromString)
                .distinct()
                .toList();
        Map<UUID, UserEntity> usersById = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> usersById.put(user.getId(), user));

        List<ActivityFeedResponse> items = feedPage.getContent().stream()
                .map(doc -> {
                    ActivityFeedResponse.UserView userView = null;
                    if (doc.getUserId() != null && !doc.getUserId().isBlank()) {
                        UserEntity user = usersById.get(UUID.fromString(doc.getUserId()));
                        if (user != null) {
                            userView = new ActivityFeedResponse.UserView(user.getId().toString(), user.getUsername(), user.getAvatarUrl());
                        }
                    }
                    return new ActivityFeedResponse(doc.getId(), doc.getType(), doc.getMessage(), userView, doc.getMetadata(), doc.getCreatedAt());
                })
                .toList();

        return new SubmissionService.PageResult<>(
                items,
                feedPage.getNumber() + 1,
                feedPage.getSize(),
                feedPage.getTotalElements(),
                feedPage.getTotalPages()
        );
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
}
