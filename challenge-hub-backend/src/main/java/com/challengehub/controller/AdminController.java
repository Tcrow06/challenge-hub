package com.challengehub.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.challengehub.dto.request.UpdateUserRoleRequest;
import com.challengehub.dto.request.UpdateUserStatusRequest;
import com.challengehub.dto.response.AdminUserResponse;
import com.challengehub.dto.response.ApiResponse;
import com.challengehub.dto.response.AuditLogResponse;
import com.challengehub.service.AdminService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getUsers(
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        AdminService.PageResult<AdminUserResponse> result = adminService.getUsers(role, status, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.items(), paginationMetadata(result)));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserRole(
            @PathVariable("id") String userId,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        AdminUserResponse response = adminService.updateUserRole(
                userId,
                request,
                currentUserId(authentication),
                currentUserRole(authentication),
                resolveClientIp(httpServletRequest),
                resolveUserAgent(httpServletRequest));
        return ResponseEntity.ok(ApiResponse.success(response, "Cap nhat role user thanh cong"));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @PathVariable("id") String userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        AdminUserResponse response = adminService.updateUserStatus(
                userId,
                request,
                currentUserId(authentication),
                currentUserRole(authentication),
                resolveClientIp(httpServletRequest),
                resolveUserAgent(httpServletRequest));
        return ResponseEntity.ok(ApiResponse.success(response, "Cap nhat trang thai user thanh cong"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogs(
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "actor_id", required = false) String actorId,
            @RequestParam(name = "resource_type", required = false) String resourceType,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        AdminService.PageResult<AuditLogResponse> result = adminService
                .getAuditLogs(action, actorId, resourceType, from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.items(), paginationMetadata(result)));
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

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            return commaIndex >= 0 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor.trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null ? "" : userAgent;
    }

    private Map<String, Object> paginationMetadata(AdminService.PageResult<?> result) {
        return Map.of(
                "page", result.page(),
                "size", result.size(),
                "totalElements", result.totalElements(),
                "totalPages", result.totalPages());
    }
}
