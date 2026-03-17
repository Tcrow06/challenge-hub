package com.challengehub.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.challengehub.dto.request.CreateChatChannelRequest;
import com.challengehub.dto.request.MarkConversationReadRequest;
import com.challengehub.dto.request.SendChatMessageRequest;
import com.challengehub.dto.request.UpdateChatMessageRequest;
import com.challengehub.dto.response.ApiResponse;
import com.challengehub.dto.response.ChatChannelCreatedResponse;
import com.challengehub.dto.response.ChatChannelResponse;
import com.challengehub.dto.response.ChatConversationSummaryResponse;
import com.challengehub.dto.response.ChatDirectConversationResponse;
import com.challengehub.dto.response.ChatMessageEditResponse;
import com.challengehub.dto.response.ChatMessageResponse;
import com.challengehub.dto.response.ChatReadReceiptResponse;
import com.challengehub.dto.response.UnreadCountResponse;
import com.challengehub.service.ChatService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ChatConversationSummaryResponse>>> getConversations(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "challenge_id", required = false) String challengeId,
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Authentication authentication) {
        ChatService.PageResult<ChatConversationSummaryResponse> result = chatService.getConversations(
                currentUserId(authentication),
                type,
                challengeId,
                keyword,
                page,
                size);
        return ResponseEntity.ok(ApiResponse.success(result.items(), Map.of(
                "page", result.page(),
                "size", result.size(),
                "totalElements", result.totalElements(),
                "totalPages", result.totalPages())));
    }

    @GetMapping("/conversations/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(Authentication authentication) {
        long count = chatService.getUnreadCount(currentUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(new UnreadCountResponse(count)));
    }

    @PostMapping("/conversations/open-direct")
    public ResponseEntity<ApiResponse<ChatDirectConversationResponse>> openDirectConversation(
            @Valid @RequestBody OpenDirectConversationRequest request,
            Authentication authentication) {
        ChatDirectConversationResponse response = chatService.openDirectConversation(
                currentUserId(authentication),
                request.targetUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/challenges/{challengeId}/channels")
    public ResponseEntity<ApiResponse<List<ChatChannelResponse>>> getChallengeChannels(
            @PathVariable("challengeId") String challengeId,
            Authentication authentication) {
        List<ChatChannelResponse> channels = chatService.getChallengeChannels(
                challengeId,
                currentUserId(authentication),
                currentUserRole(authentication));
        return ResponseEntity.ok(ApiResponse.success(channels));
    }

    @PostMapping("/channels")
    public ResponseEntity<ApiResponse<ChatChannelCreatedResponse>> createChallengeChannel(
            @Valid @RequestBody CreateChannelRequest request,
            Authentication authentication) {
        CreateChatChannelRequest serviceRequest = new CreateChatChannelRequest(
                request.channelKey(),
                request.name(),
                request.isReadonly());
        ChatChannelCreatedResponse response = chatService.createChallengeChannel(
                request.challengeId(),
                serviceRequest,
                currentUserId(authentication),
                currentUserRole(authentication));
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable("conversationId") String conversationId,
            @RequestParam(name = "before", required = false) String before,
            @RequestParam(name = "size", defaultValue = "30") int size,
            Authentication authentication) {
        ChatService.CursorResult<ChatMessageResponse> result = chatService.getConversationMessages(
                conversationId,
                before,
                size,
                currentUserId(authentication));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nextBefore", result.nextBefore());
        metadata.put("hasMore", result.hasMore());
        return ResponseEntity.ok(ApiResponse.success(result.items(), metadata));
    }

    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        SendChatMessageRequest serviceRequest = new SendChatMessageRequest(
                request.content(),
                request.attachments());
        ChatMessageResponse response = chatService.sendMessage(
                request.conversationId(),
                serviceRequest,
                currentUserId(authentication));
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @PostMapping("/messages/{id}/read")
    public ResponseEntity<ApiResponse<ChatReadReceiptResponse>> markConversationRead(
            @PathVariable("id") String messageId,
            @Valid @RequestBody ReadMessageRequest request,
            Authentication authentication) {
        MarkConversationReadRequest serviceRequest = new MarkConversationReadRequest(messageId);
        ChatReadReceiptResponse response = chatService.markConversationRead(
                request.conversationId(),
                serviceRequest,
                currentUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<ChatMessageEditResponse>> updateMessage(
            @PathVariable("messageId") String messageId,
            @Valid @RequestBody UpdateChatMessageRequest request,
            Authentication authentication) {
        ChatMessageEditResponse response = chatService.updateMessage(
                messageId,
                request,
                currentUserId(authentication));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable("messageId") String messageId,
            Authentication authentication) {
        chatService.deleteMessage(
                messageId,
                currentUserId(authentication),
                currentUserRole(authentication));
        return ResponseEntity.ok(ApiResponse.success(null, "Xoa tin nhan thanh cong"));
    }

    public record OpenDirectConversationRequest(
            @NotBlank String targetUserId) {
    }

    public record SendMessageRequest(
            @NotBlank String conversationId,
            @Size(max = 2000) String content,
            List<@Valid SendChatMessageRequest.AttachmentRequest> attachments) {
    }

    public record ReadMessageRequest(
            @NotBlank String conversationId) {
    }

    public record CreateChannelRequest(
            @NotBlank String challengeId,
            @NotBlank @Pattern(regexp = "^[a-z0-9-]{2,30}$") String channelKey,
            @NotBlank @Size(min = 2, max = 50) String name,
            Boolean isReadonly) {
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
