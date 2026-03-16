package com.challengehub.controller;

import com.challengehub.dto.response.ApiResponse;
import com.challengehub.dto.response.NotificationResponse;
import com.challengehub.dto.response.UnreadCountResponse;
import com.challengehub.service.NotificationService;
import com.challengehub.service.SubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @RequestParam(name = "unread_only", defaultValue = "false") boolean unreadOnly,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Authentication authentication
    ) {
        SubmissionService.PageResult<NotificationResponse> result = notificationService
                .getNotifications(currentUserId(authentication), unreadOnly, page, size);
        return ResponseEntity.ok(ApiResponse.success(result.items(), metadata(result)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(Authentication authentication) {
        long count = notificationService.getUnreadCount(currentUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(new UnreadCountResponse(count)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable("id") String notificationId,
            Authentication authentication
    ) {
        notificationService.markRead(notificationId, currentUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(null, "Danh dau da doc thanh cong"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(Authentication authentication) {
        notificationService.markAllRead(currentUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(null, "Danh dau tat ca da doc thanh cong"));
    }

    private String currentUserId(Authentication authentication) {
        return String.valueOf(authentication.getPrincipal());
    }

    private Map<String, Object> metadata(SubmissionService.PageResult<?> result) {
        return Map.of(
                "page", result.page(),
                "size", result.size(),
                "totalElements", result.totalElements(),
                "totalPages", result.totalPages()
        );
    }
}
