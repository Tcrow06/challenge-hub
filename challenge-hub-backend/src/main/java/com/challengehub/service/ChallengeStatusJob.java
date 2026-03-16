package com.challengehub.service;

import com.challengehub.entity.postgres.ChallengeEntity;
import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.UserChallengeEntity;
import com.challengehub.repository.postgres.ChallengeRepository;
import com.challengehub.repository.postgres.UserChallengeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class ChallengeStatusJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChallengeStatusJob.class);

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;

    public ChallengeStatusJob(ChallengeRepository challengeRepository,
                              UserChallengeRepository userChallengeRepository) {
        this.challengeRepository = challengeRepository;
        this.userChallengeRepository = userChallengeRepository;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void runChallengeLifecycle() {
        Instant now = Instant.now();

        List<ChallengeEntity> toOngoing = challengeRepository
                .findByStatusAndStartDateLessThanEqual(Enums.ChallengeStatus.PUBLISHED, now);
        for (ChallengeEntity challenge : toOngoing) {
            challenge.setStatus(Enums.ChallengeStatus.ONGOING);
        }
        challengeRepository.saveAll(toOngoing);

        List<ChallengeEntity> toEnded = challengeRepository
                .findByStatusAndEndDateLessThanEqual(Enums.ChallengeStatus.ONGOING, now);
        for (ChallengeEntity challenge : toEnded) {
            challenge.setStatus(Enums.ChallengeStatus.ENDED);
            List<UserChallengeEntity> activeParticipants = userChallengeRepository
                    .findByChallenge_IdAndStatus(challenge.getId(), Enums.UserChallengeStatus.ACTIVE);
            for (UserChallengeEntity uc : activeParticipants) {
                uc.setStatus(Enums.UserChallengeStatus.DONE);
            }
            userChallengeRepository.saveAll(activeParticipants);
        }
        challengeRepository.saveAll(toEnded);

        if (!toOngoing.isEmpty() || !toEnded.isEmpty()) {
            LOGGER.info("ChallengeStatusJob transitioned {} challenge(s) to ONGOING and {} challenge(s) to ENDED",
                    toOngoing.size(), toEnded.size());
        }
    }
}
