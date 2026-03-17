package com.challengehub.service;

import java.util.List;

import com.challengehub.dto.request.CreateChatChannelRequest;
import com.challengehub.dto.request.MarkConversationReadRequest;
import com.challengehub.dto.request.SendChatMessageRequest;
import com.challengehub.dto.request.UpdateChatMessageRequest;
import com.challengehub.dto.response.ChatChannelCreatedResponse;
import com.challengehub.dto.response.ChatChannelResponse;
import com.challengehub.dto.response.ChatConversationSummaryResponse;
import com.challengehub.dto.response.ChatDirectConversationResponse;
import com.challengehub.dto.response.ChatMessageEditResponse;
import com.challengehub.dto.response.ChatMessageResponse;
import com.challengehub.dto.response.ChatReadReceiptResponse;

public interface ChatService {

    PageResult<ChatConversationSummaryResponse> getConversations(
            String currentUserId,
            String type,
            String challengeId,
            String keyword,
            int page,
            int size);

    long getUnreadCount(String currentUserId);

    ChatDirectConversationResponse openDirectConversation(String currentUserId, String targetUserId);

    List<ChatChannelResponse> getChallengeChannels(String challengeId, String currentUserId, String currentRole);

    ChatChannelCreatedResponse createChallengeChannel(
            String challengeId,
            CreateChatChannelRequest request,
            String currentUserId,
            String currentRole);

    CursorResult<ChatMessageResponse> getConversationMessages(
            String conversationId,
            String beforeMessageId,
            int size,
            String currentUserId);

    ChatMessageResponse sendMessage(String conversationId, SendChatMessageRequest request, String currentUserId);

    ChatReadReceiptResponse markConversationRead(
            String conversationId,
            MarkConversationReadRequest request,
            String currentUserId);

    ChatMessageEditResponse updateMessage(String messageId, UpdateChatMessageRequest request, String currentUserId);

    void deleteMessage(String messageId, String currentUserId, String currentRole);

    record PageResult<T>(
            List<T> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    record CursorResult<T>(
            List<T> items,
            String nextBefore,
            boolean hasMore) {
    }
}
