package com.challengehub.service;

import com.challengehub.dto.request.UserUpdateRequest;
import com.challengehub.dto.response.UserProfileResponse;
import com.challengehub.dto.response.UserStatsResponse;

public interface UserService {

    UserProfileResponse getMyProfile(String currentUserId);

    UserProfileResponse updateMyProfile(String currentUserId, UserUpdateRequest request);

    UserStatsResponse getUserStats(String userId);
}
