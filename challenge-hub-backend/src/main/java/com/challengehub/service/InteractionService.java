package com.challengehub.service;

import com.challengehub.dto.request.CreateCommentRequest;
import com.challengehub.dto.request.ReactSubmissionRequest;
import com.challengehub.dto.response.CommentResponse;
import com.challengehub.dto.response.ReactionResponse;

public interface InteractionService {

    CommentResponse createComment(String submissionId, CreateCommentRequest request, String currentUserId);

    SubmissionService.PageResult<CommentResponse> getComments(String submissionId, int page, int size);

    ReactionResponse reactSubmission(String submissionId, ReactSubmissionRequest request, String currentUserId);

    void deleteComment(String commentId, String currentUserId, String currentRole);
}
