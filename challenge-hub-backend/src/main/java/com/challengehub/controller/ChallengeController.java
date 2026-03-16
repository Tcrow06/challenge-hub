package com.challengehub.controller;

import com.challengehub.dto.request.CreateChallengeRequest;
import com.challengehub.dto.request.CreateTaskRequest;
import com.challengehub.dto.request.UpdateChallengeStatusRequest;
import com.challengehub.dto.response.ApiResponse;
import com.challengehub.dto.response.ChallengeResponse;
import com.challengehub.dto.response.TaskResponse;
import com.challengehub.service.ChallengeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChallengeResponse>>> listChallenges() {
        return ResponseEntity.ok(ApiResponse.success(challengeService.listChallenges()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChallengeResponse>> getChallengeById(
            @PathVariable("id") String challengeId,
            Authentication authentication
    ) {
        String currentUserId = authentication == null ? null : String.valueOf(authentication.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success(challengeService.getChallengeById(challengeId, currentUserId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChallengeResponse>> createChallenge(
            @Valid @RequestBody CreateChallengeRequest request,
            Authentication authentication
    ) {
        String currentUserId = String.valueOf(authentication.getPrincipal());
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
        ChallengeResponse response = challengeService.createChallenge(request, currentUserId, role);
        return ResponseEntity.status(201).body(ApiResponse.success(response, "Tao challenge thanh cong"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ChallengeResponse>> updateChallengeStatus(
            @PathVariable("id") String challengeId,
            @Valid @RequestBody UpdateChallengeStatusRequest request,
            Authentication authentication
    ) {
        String currentUserId = String.valueOf(authentication.getPrincipal());
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
        ChallengeResponse response = challengeService.updateChallengeStatus(challengeId, request, currentUserId, role);
        return ResponseEntity.ok(ApiResponse.success(response, "Cap nhat trang thai challenge thanh cong"));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<Void>> joinChallenge(
            @PathVariable("id") String challengeId,
            Authentication authentication
    ) {
        challengeService.joinChallenge(challengeId, String.valueOf(authentication.getPrincipal()));
        return ResponseEntity.status(201).body(ApiResponse.success(null, "Tham gia challenge thanh cong"));
    }

    @PostMapping("/{id}/quit")
    public ResponseEntity<ApiResponse<Void>> quitChallenge(
            @PathVariable("id") String challengeId,
            Authentication authentication
    ) {
        challengeService.quitChallenge(challengeId, String.valueOf(authentication.getPrincipal()));
        return ResponseEntity.ok(ApiResponse.success(null, "Roi challenge thanh cong"));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
            @PathVariable("id") String challengeId,
            Authentication authentication
    ) {
        String currentUserId = authentication == null ? null : String.valueOf(authentication.getPrincipal());
        return ResponseEntity.ok(ApiResponse.success(challengeService.getTasks(challengeId, currentUserId)));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @PathVariable("id") String challengeId,
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication
    ) {
        String currentUserId = String.valueOf(authentication.getPrincipal());
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
        TaskResponse task = challengeService.createTask(challengeId, request, currentUserId, role);
        return ResponseEntity.status(201).body(ApiResponse.success(task, "Tao task thanh cong"));
    }
}
