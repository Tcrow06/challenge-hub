package com.challengehub.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.challengehub.dto.request.CreateChallengeRequest;
import com.challengehub.dto.request.CreateTaskRequest;
import com.challengehub.dto.request.UpdateChallengeStatusRequest;
import com.challengehub.dto.response.ApiResponse;
import com.challengehub.dto.response.ChallengeDetailResponse;
import com.challengehub.dto.response.ChallengeListResponse;
import com.challengehub.dto.response.ChallengeParticipationResponse;
import com.challengehub.dto.response.TaskResponse;
import com.challengehub.service.ChallengeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/challenges")
public class ChallengeController {

        private final ChallengeService challengeService;

        public ChallengeController(ChallengeService challengeService) {
                this.challengeService = challengeService;
        }

        @GetMapping
        public ResponseEntity<ApiResponse<List<ChallengeListResponse>>> listChallenges(
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        @RequestParam(name = "size", defaultValue = "10") int size) {
                ChallengeService.PageResult<ChallengeListResponse> result = challengeService.listChallenges(page, size);
                return ResponseEntity.ok(ApiResponse.success(result.items(), paginationMetadata(result)));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<ChallengeDetailResponse>> getChallengeById(
                        @PathVariable("id") String challengeId,
                        Authentication authentication) {
                String currentUserId = authentication == null ? null : String.valueOf(authentication.getPrincipal());
                return ResponseEntity
                                .ok(ApiResponse.success(challengeService.getChallengeById(challengeId, currentUserId)));
        }

        @PostMapping
        public ResponseEntity<ApiResponse<ChallengeDetailResponse>> createChallenge(
                        @Valid @RequestBody CreateChallengeRequest request,
                        Authentication authentication) {
                String currentUserId = String.valueOf(authentication.getPrincipal());
                String role = authentication.getAuthorities().stream()
                                .findFirst()
                                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                                .orElse("USER");
                ChallengeDetailResponse response = challengeService.createChallenge(request, currentUserId, role);
                return ResponseEntity.status(201).body(ApiResponse.success(response, "Tao challenge thanh cong"));
        }

        @PatchMapping("/{id}/status")
        public ResponseEntity<ApiResponse<ChallengeDetailResponse>> updateChallengeStatus(
                        @PathVariable("id") String challengeId,
                        @Valid @RequestBody UpdateChallengeStatusRequest request,
                        Authentication authentication) {
                String currentUserId = String.valueOf(authentication.getPrincipal());
                String role = authentication.getAuthorities().stream()
                                .findFirst()
                                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                                .orElse("USER");
                ChallengeDetailResponse response = challengeService.updateChallengeStatus(challengeId, request,
                                currentUserId, role);
                return ResponseEntity.ok(ApiResponse.success(response, "Cap nhat trang thai challenge thanh cong"));
        }

        @PostMapping("/{id}/join")
        public ResponseEntity<ApiResponse<ChallengeParticipationResponse>> joinChallenge(
                        @PathVariable("id") String challengeId,
                        Authentication authentication) {
                ChallengeParticipationResponse response = challengeService
                                .joinChallenge(challengeId, String.valueOf(authentication.getPrincipal()));
                return ResponseEntity.status(201).body(ApiResponse.success(response));
        }

        @PostMapping("/{id}/quit")
        public ResponseEntity<ApiResponse<ChallengeParticipationResponse>> quitChallenge(
                        @PathVariable("id") String challengeId,
                        Authentication authentication) {
                ChallengeParticipationResponse response = challengeService
                                .quitChallenge(challengeId, String.valueOf(authentication.getPrincipal()));
                return ResponseEntity.ok(ApiResponse.success(response));
        }

        @GetMapping("/{id}/tasks")
        public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
                        @PathVariable("id") String challengeId,
                        Authentication authentication) {
                String currentUserId = authentication == null ? null : String.valueOf(authentication.getPrincipal());
                return ResponseEntity.ok(ApiResponse.success(challengeService.getTasks(challengeId, currentUserId)));
        }

        @PostMapping("/{id}/tasks")
        public ResponseEntity<ApiResponse<TaskResponse>> createTask(
                        @PathVariable("id") String challengeId,
                        @Valid @RequestBody CreateTaskRequest request,
                        Authentication authentication) {
                String currentUserId = String.valueOf(authentication.getPrincipal());
                String role = authentication.getAuthorities().stream()
                                .findFirst()
                                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                                .orElse("USER");
                TaskResponse task = challengeService.createTask(challengeId, request, currentUserId, role);
                return ResponseEntity.status(201).body(ApiResponse.success(task, "Tao task thanh cong"));
        }

        private Map<String, Object> paginationMetadata(ChallengeService.PageResult<?> result) {
                return Map.of(
                                "page", result.page(),
                                "size", result.size(),
                                "totalElements", result.totalElements(),
                                "totalPages", result.totalPages());
        }
}
