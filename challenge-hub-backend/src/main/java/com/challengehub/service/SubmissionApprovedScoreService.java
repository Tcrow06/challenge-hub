package com.challengehub.service;

import com.challengehub.event.SubmissionApprovedEvent;

public interface SubmissionApprovedScoreService {

    boolean applyScore(SubmissionApprovedEvent event);
}
