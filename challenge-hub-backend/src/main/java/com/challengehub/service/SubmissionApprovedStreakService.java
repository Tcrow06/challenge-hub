package com.challengehub.service;

import com.challengehub.event.SubmissionApprovedEvent;

public interface SubmissionApprovedStreakService {

    void updateStreak(SubmissionApprovedEvent event);
}
