package com.challengehub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.challengehub.dto.request.UserUpdateRequest;
import com.challengehub.dto.response.ApiResponse;
import com.challengehub.dto.response.UserProfileResponse;
import com.challengehub.dto.response.UserStatsResponse;
import com.challengehub.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(Authentication authentication) {
        UserProfileResponse response = userService.getMyProfile(currentUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {
        UserProfileResponse response = userService.updateMyProfile(currentUserId(authentication), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cap nhat profile thanh cong"));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats(@PathVariable("id") String userId) {
        UserStatsResponse response = userService.getUserStats(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String currentUserId(Authentication authentication) {
        return String.valueOf(authentication.getPrincipal());
    }
}
