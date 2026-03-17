package com.challengehub.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.challengehub.entity.mongodb.ActivityFeedDocument;
import com.challengehub.repository.mongodb.ActivityFeedRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ActivityFeedServiceImplTest {

    @Mock
    private ActivityFeedRepository activityFeedRepository;

    @InjectMocks
    private ActivityFeedServiceImpl activityFeedService;

    @Test
    void createFeedShouldPersistWhenNotExisting() {
        when(activityFeedRepository.existsByUserIdAndTypeAndReferenceId(
                "user-1",
                "JOIN_CHALLENGE",
                "challenge-1")).thenReturn(false);

        boolean created = activityFeedService.createFeed("user-1", "JOIN_CHALLENGE", "challenge-1");

        assertThat(created).isTrue();
        verify(activityFeedRepository, times(1)).save(any(ActivityFeedDocument.class));
    }

    @Test
    void createFeedShouldSkipWhenAlreadyExists() {
        when(activityFeedRepository.existsByUserIdAndTypeAndReferenceId(
                "user-1",
                "JOIN_CHALLENGE",
                "challenge-1")).thenReturn(true);

        boolean created = activityFeedService.createFeed("user-1", "JOIN_CHALLENGE", "challenge-1");

        assertThat(created).isFalse();
        verify(activityFeedRepository, never()).save(any(ActivityFeedDocument.class));
    }

    @Test
    void createFeedShouldHandleDuplicateKeyRaceSafely() {
        when(activityFeedRepository.existsByUserIdAndTypeAndReferenceId(
                "user-1",
                "JOIN_CHALLENGE",
                "challenge-1")).thenReturn(false);
        when(activityFeedRepository.save(any(ActivityFeedDocument.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        boolean created = activityFeedService.createFeed("user-1", "JOIN_CHALLENGE", "challenge-1");

        assertThat(created).isFalse();
    }
}
