package com.challengehub.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.challengehub.dto.request.UpdateSubmissionStatusRequest;
import com.challengehub.dto.response.SubmissionResponse;
import com.challengehub.entity.postgres.ChallengeEntity;
import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.SubmissionEntity;
import com.challengehub.entity.postgres.TaskEntity;
import com.challengehub.entity.postgres.UserEntity;
import com.challengehub.event.EventPublisher;
import com.challengehub.event.SubmissionApprovedEvent;
import com.challengehub.repository.postgres.MediaRepository;
import com.challengehub.repository.postgres.SubmissionRepository;
import com.challengehub.repository.postgres.TaskRepository;
import com.challengehub.repository.postgres.UserChallengeRepository;
import com.challengehub.repository.postgres.UserRepository;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private UserChallengeRepository userChallengeRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    @Test
    @SuppressWarnings("null")
    void approvingSameSubmissionTwiceShouldPublishApprovalEventOnlyOnce() {
        UUID submissionId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();

        UserEntity submitter = new UserEntity();
        submitter.setId(userId);
        submitter.setStreakCount(0);

        UserEntity reviewer = new UserEntity();
        reviewer.setId(reviewerId);

        ChallengeEntity challenge = new ChallengeEntity();
        challenge.setId(challengeId);

        TaskEntity task = new TaskEntity();
        task.setId(taskId);
        task.setChallenge(challenge);
        task.setMaxScore(10);

        SubmissionEntity submission = new SubmissionEntity();
        submission.setId(submissionId);
        submission.setTask(task);
        submission.setUser(submitter);
        submission.setStatus(Enums.SubmissionStatus.PENDING);

        when(submissionRepository.findByIdForUpdate(submissionId)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any(SubmissionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));

        UpdateSubmissionStatusRequest approveRequest = new UpdateSubmissionStatusRequest(
                Enums.SubmissionStatus.APPROVED,
                8,
                null);

        SubmissionResponse firstResponse = submissionService.updateSubmissionStatus(
                submissionId.toString(),
                approveRequest,
                reviewerId.toString(),
                "MODERATOR");

        SubmissionResponse secondResponse = submissionService.updateSubmissionStatus(
                submissionId.toString(),
                approveRequest,
                reviewerId.toString(),
                "MODERATOR");

        assertThat(firstResponse.status()).isEqualTo(Enums.SubmissionStatus.APPROVED);
        assertThat(secondResponse.status()).isEqualTo(Enums.SubmissionStatus.APPROVED);
        assertThat(firstResponse.score()).isEqualTo(8);
        assertThat(secondResponse.score()).isEqualTo(8);

        verify(eventPublisher, times(1)).publish(any(SubmissionApprovedEvent.class));
    }
}
