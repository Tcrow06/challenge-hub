package com.challengehub.service;

import java.time.Instant;
import java.util.List;

import com.challengehub.dto.request.UpdateUserRoleRequest;
import com.challengehub.dto.request.UpdateUserStatusRequest;
import com.challengehub.dto.response.AdminUserResponse;
import com.challengehub.dto.response.AuditLogResponse;

public interface AdminService {

    PageResult<AdminUserResponse> getUsers(String role, String status, String keyword, int page, int size);

    AdminUserResponse updateUserRole(
            String userId,
            UpdateUserRoleRequest request,
            String actorId,
            String actorRole,
            String ipAddress,
            String userAgent);

    AdminUserResponse updateUserStatus(
            String userId,
            UpdateUserStatusRequest request,
            String actorId,
            String actorRole,
            String ipAddress,
            String userAgent);

    PageResult<AuditLogResponse> getAuditLogs(
            String action,
            String actorId,
            String resourceType,
            Instant from,
            Instant to,
            int page,
            int size);

    record PageResult<T>(
            List<T> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }
}
