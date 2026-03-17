package com.challengehub.service.impl;

import java.time.Instant;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.challengehub.entity.mongodb.ActivityFeedDocument;
import com.challengehub.repository.mongodb.ActivityFeedRepository;
import com.challengehub.service.ActivityFeedService;

@Service
public class ActivityFeedServiceImpl implements ActivityFeedService {

    private final ActivityFeedRepository activityFeedRepository;

    public ActivityFeedServiceImpl(ActivityFeedRepository activityFeedRepository) {
        this.activityFeedRepository = activityFeedRepository;
    }

    @Override
    public boolean createFeed(String userId, String type, String referenceId) {
        String normalizedUserId = trimToNull(userId);
        String normalizedType = trimToNull(type);
        String normalizedReferenceId = trimToNull(referenceId);
        if (normalizedUserId == null || normalizedType == null || normalizedReferenceId == null) {
            return false;
        }

        if (activityFeedRepository.existsByUserIdAndTypeAndReferenceId(
                normalizedUserId,
                normalizedType,
                normalizedReferenceId)) {
            return false;
        }

        ActivityFeedDocument activityFeed = new ActivityFeedDocument();
        activityFeed.setUserId(normalizedUserId);
        activityFeed.setType(normalizedType);
        activityFeed.setReferenceId(normalizedReferenceId);
        activityFeed.setCreatedAt(Instant.now());

        try {
            activityFeedRepository.save(activityFeed);
            return true;
        } catch (DuplicateKeyException duplicateKeyException) {
            return false;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}