package com.challengehub.service.impl;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.challengehub.entity.postgres.UserEntity;
import com.challengehub.event.SubmissionApprovedEvent;
import com.challengehub.exception.ApiException;
import com.challengehub.repository.postgres.UserRepository;
import com.challengehub.service.SubmissionApprovedStreakService;

@Service
@Transactional
public class SubmissionApprovedStreakServiceImpl implements SubmissionApprovedStreakService {

    private final UserRepository userRepository;

    public SubmissionApprovedStreakServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void updateStreak(SubmissionApprovedEvent event) {
        UserEntity user = userRepository
                .findByIdForUpdate(Objects.requireNonNull(UUID.fromString(event.getUserId())))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.NOT_FOUND,
                        "Khong tim thay nguoi dung"));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate lastDate = user.getStreakLastDate();

        if (today.equals(lastDate)) {
            return;
        }

        if (lastDate != null && lastDate.equals(today.minusDays(1))) {
            int currentStreak = user.getStreakCount() == null ? 0 : user.getStreakCount();
            user.setStreakCount(currentStreak + 1);
        } else {
            user.setStreakCount(1);
        }

        user.setStreakLastDate(today);
        userRepository.save(user);
    }
}
