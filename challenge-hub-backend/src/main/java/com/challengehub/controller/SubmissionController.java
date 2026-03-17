package com.challengehub.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.challengehub.dto.request.CreateSubmissionRequest;
import com.challengehub.dto.request.UpdateSubmissionStatusRequest;
import com.challengehub.dto.response.ApiResponse;
import com.challengehub.dto.response.SubmissionCreateResponse;
import com.challengehub.dto.response.SubmissionResponse;
import com.challengehub.service.SubmissionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

        private final SubmissionService submissionService;

        public SubmissionController(SubmissionService submissionService) {
                this.submissionService = submissionService;
        }

        @PostMapping("/tasks/{taskId}/submit")
        public ResponseEntity<ApiResponse<SubmissionCreateResponse>> submitTask(
                        @PathVariable String taskId,
                        @Valid @RequestBody CreateSubmissionRequest request,
                        Authentication authentication) {
                SubmissionCreateResponse response = submissionService.submitTask(taskId, request,
                                currentUserId(authentication));
                return ResponseEntity.status(201).body(ApiResponse.success(response, "Nop bai thanh cong"));
        }

        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<SubmissionResponse>> resubmit(
                        @PathVariable("id") String submissionId,
                        @Valid @RequestBody CreateSubmissionRequest request,
                        Authentication authentication) {
                SubmissionResponse response = submissionService.resubmit(submissionId, request,
                                currentUserId(authentication));
                return ResponseEntity.ok(ApiResponse.success(response, "Cap nhat bai nop thanh cong"));
        }

        @PatchMapping("/{id}/status")
        public ResponseEntity<ApiResponse<SubmissionResponse>> updateStatus(
                        @PathVariable("id") String submissionId,
                        @Valid @RequestBody UpdateSubmissionStatusRequest request,
                        Authentication authentication) {
                SubmissionResponse response = submissionService.updateSubmissionStatus(
                                submissionId,
                                request,
                                currentUserId(authentication),
                                currentUserRole(authentication));
                return ResponseEntity.ok(ApiResponse.success(response, "Cap nhat trang thai bai nop thanh cong"));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<SubmissionResponse>> getById(
                        @PathVariable("id") String submissionId,
                        Authentication authentication) {
                SubmissionResponse response = submissionService.getSubmissionById(
                                submissionId,
                                currentUserId(authentication),
                                currentUserRole(authentication));
                return ResponseEntity.ok(ApiResponse.success(response));
        }

        @GetMapping("/me")
        public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getMySubmissions(
                        @RequestParam(name = "challenge_id", required = false) String challengeId,
                        @RequestParam(name = "status", required = false) String status,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        @RequestParam(name = "size", defaultValue = "10") int size,
                        Authentication authentication) {
                SubmissionService.PageResult<SubmissionResponse> result = submissionService
                                .getMySubmissions(currentUserId(authentication), challengeId, status, page, size);
                return ResponseEntity.ok(ApiResponse.success(result.items(), paginationMetadata(result)));
        }

        @GetMapping("/pending")
        public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getPendingSubmissions(
                        @RequestParam(name = "challenge_id", required = false) String challengeId,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        @RequestParam(name = "size", defaultValue = "10") int size,
                        Authentication authentication) {
                SubmissionService.PageResult<SubmissionResponse> result = submissionService
                                .getPendingSubmissions(currentUserRole(authentication), challengeId, page, size);
                return ResponseEntity.ok(ApiResponse.success(result.items(), paginationMetadata(result)));
        }

        private Map<String, Object> paginationMetadata(SubmissionService.PageResult<?> result) {
                return Map.of(
                                "page", result.page(),
                                "size", result.size(),
                                "totalElements", result.totalElements(),
                                "totalPages", result.totalPages());
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
}
