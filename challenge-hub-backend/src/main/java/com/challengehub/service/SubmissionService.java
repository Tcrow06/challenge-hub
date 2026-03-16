package com.challengehub.service;

import com.challengehub.dto.request.CreateSubmissionRequest;
import com.challengehub.dto.request.UpdateSubmissionStatusRequest;
import com.challengehub.dto.response.SubmissionResponse;

import java.util.List;

public interface SubmissionService {

    SubmissionResponse submitTask(String taskId, CreateSubmissionRequest request, String currentUserId);

    SubmissionResponse resubmit(String submissionId, CreateSubmissionRequest request, String currentUserId);

    SubmissionResponse updateSubmissionStatus(String submissionId, UpdateSubmissionStatusRequest request, String reviewerId, String reviewerRole);

    SubmissionResponse getSubmissionById(String submissionId, String currentUserId, String currentUserRole);

    PageResult<SubmissionResponse> getMySubmissions(String currentUserId, String challengeId, String status, int page, int size);

    PageResult<SubmissionResponse> getPendingSubmissions(String currentUserRole, String challengeId, int page, int size);

    record PageResult<T>(
            List<T> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
