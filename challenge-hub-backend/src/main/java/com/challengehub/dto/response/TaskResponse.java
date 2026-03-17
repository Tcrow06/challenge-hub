package com.challengehub.dto.response;

import com.challengehub.entity.postgres.Enums;

public record TaskResponse(
                String id,
                Integer dayNumber,
                String title,
                String content,
                Integer maxScore,
                boolean isUnlocked,
                MySubmissionView mySubmission) {

        public record MySubmissionView(
                        String id,
                        Enums.SubmissionStatus status,
                        Integer score) {
        }
}
