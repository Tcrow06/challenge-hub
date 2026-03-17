package com.challengehub.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.challengehub.dto.request.CreateCommentRequest;
import com.challengehub.dto.request.ReactSubmissionRequest;
import com.challengehub.dto.response.ApiResponse;
import com.challengehub.dto.response.CommentResponse;
import com.challengehub.dto.response.ReactionResponse;
import com.challengehub.service.InteractionService;
import com.challengehub.service.SubmissionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/interactions")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping("/submissions/{id}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable("id") String submissionId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        CommentResponse response = interactionService.createComment(submissionId, request,
                currentUserId(authentication));
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @GetMapping("/submissions/{id}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable("id") String submissionId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        SubmissionService.PageResult<CommentResponse> result = interactionService.getComments(submissionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.items(), metadata(result)));
    }

    @PostMapping("/submissions/{id}/react")
    public ResponseEntity<ApiResponse<ReactionResponse>> reactSubmission(
            @PathVariable("id") String submissionId,
            @Valid @RequestBody ReactSubmissionRequest request,
            Authentication authentication) {
        ReactionResponse response = interactionService.reactSubmission(submissionId, request,
                currentUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable String commentId,
            Authentication authentication) {
        interactionService.deleteComment(commentId, currentUserId(authentication), currentUserRole(authentication));
        return ResponseEntity.ok(ApiResponse.success(null, "Xoa comment thanh cong"));
    }

    private String currentUserId(Authentication authentication) {
        return String.valueOf(authentication.getPrincipal());
    }

    private String currentUserRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
    }

    private Map<String, Object> metadata(SubmissionService.PageResult<?> result) {
        return Map.of(
                "page", result.page(),
                "size", result.size(),
                "totalElements", result.totalElements(),
                "totalPages", result.totalPages());
    }
}
