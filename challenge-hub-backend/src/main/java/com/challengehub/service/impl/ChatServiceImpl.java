package com.challengehub.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.challengehub.entity.mongodb.ChatConversationDocument;
import com.challengehub.entity.mongodb.ChatMembershipDocument;
import com.challengehub.entity.mongodb.ChatMessageDocument;
import com.challengehub.entity.postgres.ChallengeEntity;
import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.MediaEntity;
import com.challengehub.entity.postgres.UserChallengeEntity;
import com.challengehub.entity.postgres.UserEntity;
import com.challengehub.exception.ApiException;
import com.challengehub.exception.ErrorCode;
import com.challengehub.repository.mongodb.ChatConversationRepository;
import com.challengehub.repository.mongodb.ChatMembershipRepository;
import com.challengehub.repository.mongodb.ChatMessageRepository;
import com.challengehub.repository.postgres.ChallengeRepository;
import com.challengehub.repository.postgres.MediaRepository;
import com.challengehub.repository.postgres.UserChallengeRepository;
import com.challengehub.repository.postgres.UserRepository;
import com.challengehub.service.ChatService;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private static final String TYPE_DIRECT = "DIRECT";
    private static final String TYPE_CHANNEL = "CHALLENGE_CHANNEL";
    private static final Duration MESSAGE_EDIT_WINDOW = Duration.ofMinutes(15);

    private final ChatConversationRepository chatConversationRepository;
    private final ChatMembershipRepository chatMembershipRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final MediaRepository mediaRepository;

    public ChatServiceImpl(ChatConversationRepository chatConversationRepository,
            ChatMembershipRepository chatMembershipRepository,
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            ChallengeRepository challengeRepository,
            UserChallengeRepository userChallengeRepository,
            MediaRepository mediaRepository) {
        this.chatConversationRepository = chatConversationRepository;
        this.chatMembershipRepository = chatMembershipRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
        this.userChallengeRepository = userChallengeRepository;
        this.mediaRepository = mediaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ChatConversationSummaryResponse> getConversations(String currentUserId,
            String type,
            String challengeId,
            String keyword,
            int page,
            int size) {
        String normalizedType = normalizeConversationType(type);
        String normalizedKeyword = normalizeKeyword(keyword);
        UUID challengeFilter = parseOptionalUuid(challengeId, "challengeId");
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size, 10, 50);

        List<ChatMembershipDocument> myMemberships = chatMembershipRepository
                .findByUserIdAndLeftAtIsNullOrderByUpdatedAtDesc(currentUserId);
        if (myMemberships.isEmpty()) {
            return new PageResult<>(List.of(), normalizedPage, normalizedSize, 0, 0);
        }

        Map<String, ChatMembershipDocument> membershipByConversationId = myMemberships.stream()
                .collect(Collectors.toMap(
                        ChatMembershipDocument::getConversationId,
                        Function.identity(),
                        (existing, ignored) -> existing,
                        LinkedHashMap::new));

        List<String> conversationIds = new ArrayList<>(membershipByConversationId.keySet());
        if (conversationIds.isEmpty()) {
            return new PageResult<>(List.of(), normalizedPage, normalizedSize, 0, 0);
        }

        List<ChatConversationDocument> conversations = chatConversationRepository.findAllById(conversationIds)
                .stream()
                .filter(Objects::nonNull)
                .filter(conversation -> !conversation.isArchived())
                .toList();

        if (conversations.isEmpty()) {
            return new PageResult<>(List.of(), normalizedPage, normalizedSize, 0, 0);
        }

        List<ChatMembershipDocument> activeMemberships = chatMembershipRepository
                .findByConversationIdInAndLeftAtIsNull(conversationIds);
        Map<String, List<ChatMembershipDocument>> membershipsByConversationId = activeMemberships.stream()
                .collect(Collectors.groupingBy(ChatMembershipDocument::getConversationId));

        Set<String> counterpartUserIds = conversations.stream()
                .filter(conversation -> TYPE_DIRECT.equals(conversation.getType()))
                .map(ChatConversationDocument::getId)
                .map(conversationIdValue -> resolveCounterpartId(conversationIdValue, currentUserId,
                        membershipsByConversationId.getOrDefault(conversationIdValue, List.of())))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        Map<String, UserEntity> counterpartsById = findUsersById(counterpartUserIds);

        Set<String> challengeIds = conversations.stream()
                .map(ChatConversationDocument::getChallengeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, ChallengeEntity> challengesById = findChallengesById(challengeIds);

        List<ChatConversationSummaryResponse> items = conversations.stream()
                .filter(conversation -> filterByType(conversation, normalizedType))
                .filter(conversation -> filterByChallenge(conversation, challengeFilter))
                .map(conversation -> toConversationSummary(
                        conversation,
                        membershipByConversationId.get(conversation.getId()),
                        membershipsByConversationId,
                        counterpartsById,
                        challengesById,
                        currentUserId))
                .filter(Objects::nonNull)
                .filter(item -> filterByKeyword(item, normalizedKeyword))
                .sorted(Comparator.comparing(
                        ChatConversationSummaryResponse::updatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        long totalElements = items.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / normalizedSize);

        int fromIndex = Math.min((normalizedPage - 1) * normalizedSize, items.size());
        int toIndex = Math.min(fromIndex + normalizedSize, items.size());
        List<ChatConversationSummaryResponse> pagedItems = items.subList(fromIndex, toIndex);

        return new PageResult<>(pagedItems, normalizedPage, normalizedSize, totalElements, totalPages);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String currentUserId) {
        return chatMembershipRepository.findByUserIdAndLeftAtIsNullOrderByUpdatedAtDesc(currentUserId)
                .stream()
                .mapToLong(ChatMembershipDocument::getUnreadCount)
                .sum();
    }

    @Override
    public ChatDirectConversationResponse openDirectConversation(String currentUserId, String targetUserId) {
        UUID actorId = parseUuid(currentUserId, "currentUserId");
        UUID targetId = parseUuid(targetUserId, "targetUserId");
        if (actorId.equals(targetId)) {
            throw new ApiException(ErrorCode.CHAT_DM_SELF_NOT_ALLOWED,
                    "Khong the tao cuoc tro chuyen rieng voi chinh minh");
        }

        UserEntity targetUser = findUser(targetId.toString());
        String participantsHash = participantsHash(actorId, targetId);

        Instant now = Instant.now();
        boolean created = false;

        ChatConversationDocument conversation = chatConversationRepository
                .findByParticipantsHashAndType(participantsHash, TYPE_DIRECT)
                .orElseGet(() -> {
                    ChatConversationDocument newConversation = new ChatConversationDocument();
                    newConversation.setType(TYPE_DIRECT);
                    newConversation.setCreatedBy(actorId.toString());
                    newConversation.setParticipantsHash(participantsHash);
                    newConversation.setCreatedAt(now);
                    newConversation.setUpdatedAt(now);
                    newConversation.setArchived(false);
                    return newConversation;
                });

        if (conversation.getId() == null) {
            created = true;
        }

        if (conversation.isArchived()) {
            conversation.setArchived(false);
        }
        if (conversation.getCreatedAt() == null) {
            conversation.setCreatedAt(now);
        }
        conversation.setUpdatedAt(now);

        conversation = chatConversationRepository.save(conversation);

        upsertMembership(conversation.getId(), actorId.toString(), "OWNER", now);
        upsertMembership(conversation.getId(), targetId.toString(), "MEMBER", now);

        ChatConversationSummaryResponse.CounterpartView counterpart = new ChatConversationSummaryResponse.CounterpartView(
                targetUser.getId().toString(),
                targetUser.getUsername(),
                targetUser.getAvatarUrl());

        return new ChatDirectConversationResponse(
                conversation.getId(),
                TYPE_DIRECT,
                counterpart,
                created);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatChannelResponse> getChallengeChannels(String challengeId, String currentUserId,
            String currentRole) {
        ChallengeEntity challenge = findChallenge(challengeId);
        ensureCanViewChallengeChannels(challenge, currentUserId, currentRole);

        return chatConversationRepository
                .findByChallengeIdAndTypeAndArchivedFalseOrderByUpdatedAtDesc(challenge.getId().toString(),
                        TYPE_CHANNEL)
                .stream()
                .map(conversation -> new ChatChannelResponse(
                        conversation.getId(),
                        conversation.getChannelKey(),
                        conversation.getChannelName(),
                        conversation.isDefaultChannel(),
                        conversation.isReadOnly(),
                        chatMembershipRepository.countByConversationIdAndLeftAtIsNull(conversation.getId())))
                .toList();
    }

    @Override
    public ChatChannelCreatedResponse createChallengeChannel(String challengeId,
            CreateChatChannelRequest request,
            String currentUserId,
            String currentRole) {
        ChallengeEntity challenge = findChallenge(challengeId);
        ensureCanCreateChallengeChannel(challenge, currentUserId, currentRole);

        String channelKey = request.channelKey().trim().toLowerCase(Locale.ROOT);
        String channelName = request.name().trim();

        chatConversationRepository
                .findByChallengeIdAndChannelKeyAndType(challenge.getId().toString(), channelKey, TYPE_CHANNEL)
                .ifPresent(existing -> {
                    throw new ApiException(ErrorCode.CHAT_CHANNEL_ALREADY_EXISTS,
                            "Channel key da ton tai trong challenge");
                });

        Instant now = Instant.now();

        ChatConversationDocument conversation = new ChatConversationDocument();
        conversation.setType(TYPE_CHANNEL);
        conversation.setChallengeId(challenge.getId().toString());
        conversation.setChannelKey(channelKey);
        conversation.setChannelName(channelName);
        conversation.setDefaultChannel(false);
        conversation.setReadOnly(Boolean.TRUE.equals(request.isReadonly()));
        conversation.setCreatedBy(currentUserId);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversation.setArchived(false);

        conversation = chatConversationRepository.save(conversation);
        syncChallengeMemberships(challenge, conversation, now);

        return new ChatChannelCreatedResponse(
                conversation.getId(),
                challenge.getId().toString(),
                conversation.getChannelKey(),
                conversation.getChannelName(),
                conversation.isDefaultChannel(),
                conversation.isReadOnly(),
                conversation.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public CursorResult<ChatMessageResponse> getConversationMessages(String conversationId,
            String beforeMessageId,
            int size,
            String currentUserId) {
        ChatConversationDocument conversation = findConversation(conversationId);
        requireActiveMembership(conversation.getId(), currentUserId);

        int normalizedSize = normalizeSize(size, 30, 100);
        Pageable pageable = PageRequest.of(0, normalizedSize + 1, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<ChatMessageDocument> documents;
        if (beforeMessageId != null && !beforeMessageId.isBlank()) {
            Optional<ChatMessageDocument> beforeMessage = chatMessageRepository
                    .findByIdAndConversationId(beforeMessageId, conversation.getId());
            if (beforeMessage.isPresent()) {
                documents = chatMessageRepository.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                        conversation.getId(),
                        beforeMessage.get().getCreatedAt(),
                        pageable);
            } else {
                documents = chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(),
                        pageable);
            }
        } else {
            documents = chatMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), pageable);
        }

        boolean hasMore = documents.size() > normalizedSize;
        List<ChatMessageDocument> payload = hasMore
                ? documents.subList(0, normalizedSize)
                : documents;

        String nextBefore = hasMore && !payload.isEmpty() ? payload.get(payload.size() - 1).getId() : null;

        List<ChatMessageResponse> items = payload.stream()
                .map(this::toMessageResponse)
                .toList();

        return new CursorResult<>(items, nextBefore, hasMore);
    }

    @Override
    public ChatMessageResponse sendMessage(String conversationId, SendChatMessageRequest request,
            String currentUserId) {
        ChatConversationDocument conversation = findConversation(conversationId);
        ChatMembershipDocument senderMembership = requireActiveMembership(conversation.getId(), currentUserId);
        UserEntity sender = findUser(currentUserId);

        if (sender.getStatus() != Enums.UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Tai khoan hien tai khong duoc phep gui tin nhan");
        }

        if (conversation.isReadOnly() && !isModeratorOrAdmin(sender.getRole().name())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Channel dang o che do chi doc");
        }

        String normalizedContent = trimToNull(request.content());
        List<ChatMessageDocument.AttachmentSnapshot> attachments = resolveAttachments(request.attachments(),
                currentUserId);

        if (normalizedContent == null && attachments.isEmpty()) {
            throw new ApiException(ErrorCode.CHAT_EMPTY_MESSAGE,
                    "Message phai co noi dung hoac attachment hop le");
        }

        Instant now = Instant.now();
        ChatMessageDocument message = new ChatMessageDocument();
        message.setConversationId(conversation.getId());
        message.setSenderId(sender.getId().toString());
        message.setSenderUsername(sender.getUsername());
        message.setSenderAvatarUrl(sender.getAvatarUrl());
        message.setType(resolveMessageType(normalizedContent, attachments));
        message.setContent(normalizedContent);
        message.setAttachments(attachments);
        message.setCreatedAt(now);

        message = chatMessageRepository.save(message);

        ChatConversationDocument.LastMessageSnapshot snapshot = new ChatConversationDocument.LastMessageSnapshot();
        snapshot.setMessageId(message.getId());
        snapshot.setSenderId(message.getSenderId());
        snapshot.setContentPreview(buildContentPreview(message.getContent(), message.getAttachments(), false));
        snapshot.setSentAt(now);
        conversation.setLastMessage(snapshot);
        conversation.setUpdatedAt(now);
        chatConversationRepository.save(conversation);

        senderMembership.setUnreadCount(0);
        senderMembership.setLastReadMessageId(message.getId());
        senderMembership.setLastReadAt(now);
        senderMembership.setUpdatedAt(now);
        chatMembershipRepository.save(senderMembership);

        List<ChatMembershipDocument> otherMemberships = chatMembershipRepository
                .findByConversationIdAndLeftAtIsNullAndUserIdNot(conversation.getId(), currentUserId);
        for (ChatMembershipDocument membership : otherMemberships) {
            membership.setUnreadCount(membership.getUnreadCount() + 1);
            membership.setUpdatedAt(now);
        }
        if (!otherMemberships.isEmpty()) {
            chatMembershipRepository.saveAll(otherMemberships);
        }

        return toMessageResponse(message);
    }

    @Override
    public ChatReadReceiptResponse markConversationRead(String conversationId,
            MarkConversationReadRequest request,
            String currentUserId) {
        ChatConversationDocument conversation = findConversation(conversationId);
        ChatMembershipDocument membership = requireActiveMembership(conversation.getId(), currentUserId);

        ChatMessageDocument lastMessage = chatMessageRepository
                .findByIdAndConversationId(request.lastMessageId(), conversation.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_MESSAGE_NOT_FOUND, "Khong tim thay tin nhan"));

        Instant now = Instant.now();
        membership.setLastReadMessageId(lastMessage.getId());
        membership.setLastReadAt(now);
        membership.setUnreadCount(0);
        membership.setUpdatedAt(now);
        chatMembershipRepository.save(membership);

        return new ChatReadReceiptResponse(
                conversation.getId(),
                lastMessage.getId(),
                0,
                now);
    }

    @Override
    public ChatMessageEditResponse updateMessage(String messageId,
            UpdateChatMessageRequest request,
            String currentUserId) {
        ChatMessageDocument message = findMessage(messageId);
        if (!currentUserId.equals(message.getSenderId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Ban khong co quyen sua tin nhan nay");
        }
        ensureEditWindow(message);

        String normalizedContent = trimToNull(request.content());
        if (normalizedContent == null) {
            throw new ApiException(ErrorCode.CHAT_EMPTY_MESSAGE, "Noi dung tin nhan khong duoc de trong");
        }

        Instant now = Instant.now();
        message.setContent(normalizedContent);
        message.setEditedAt(now);
        message = chatMessageRepository.save(message);

        refreshConversationLastMessagePreview(message, false);

        return new ChatMessageEditResponse(
                message.getId(),
                message.getConversationId(),
                message.getContent(),
                message.getEditedAt());
    }

    @Override
    public void deleteMessage(String messageId, String currentUserId, String currentRole) {
        ChatMessageDocument message = findMessage(messageId);

        boolean owner = currentUserId.equals(message.getSenderId());
        boolean moderatorOrAdmin = isModeratorOrAdmin(currentRole);
        if (!owner && !moderatorOrAdmin) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Ban khong co quyen xoa tin nhan nay");
        }
        if (owner && !moderatorOrAdmin) {
            ensureEditWindow(message);
        }

        if (message.getDeletedAt() != null) {
            return;
        }

        message.setDeletedAt(Instant.now());
        message.setContent(null);
        message.setAttachments(List.of());
        chatMessageRepository.save(message);

        refreshConversationLastMessagePreview(message, true);
    }

    private ChatConversationDocument findConversation(String conversationId) {
        ChatConversationDocument conversation = chatConversationRepository
                .findById(Objects.requireNonNull(conversationId))
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_CONVERSATION_NOT_FOUND,
                        "Khong tim thay conversation"));
        if (conversation.isArchived()) {
            throw new ApiException(ErrorCode.CHAT_CONVERSATION_NOT_FOUND, "Khong tim thay conversation");
        }
        return conversation;
    }

    private ChatMessageDocument findMessage(String messageId) {
        return chatMessageRepository.findById(Objects.requireNonNull(messageId))
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_MESSAGE_NOT_FOUND,
                        "Khong tim thay tin nhan"));
    }

    private ChallengeEntity findChallenge(String challengeId) {
        UUID id = parseUuid(challengeId, "challengeId");
        return challengeRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ApiException(ErrorCode.CHALLENGE_NOT_FOUND,
                        "Khong tim thay challenge"));
    }

    private UserEntity findUser(String userId) {
        UUID id = parseUuid(userId, "userId");
        return userRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Khong tim thay nguoi dung"));
    }

    private ChatMembershipDocument requireActiveMembership(String conversationId, String userId) {
        return chatMembershipRepository.findByConversationIdAndUserIdAndLeftAtIsNull(conversationId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHAT_MEMBER_REQUIRED,
                        "Ban khong phai thanh vien cua conversation"));
    }

    private ChatMembershipDocument upsertMembership(String conversationId, String userId, String role, Instant now) {
        ChatMembershipDocument membership = chatMembershipRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseGet(ChatMembershipDocument::new);

        membership.setConversationId(conversationId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setMuted(false);
        membership.setLeftAt(null);
        if (membership.getJoinedAt() == null) {
            membership.setJoinedAt(now);
        }
        membership.setUnreadCount(0);
        membership.setUpdatedAt(now);

        return chatMembershipRepository.save(membership);
    }

    private void syncChallengeMemberships(ChallengeEntity challenge,
            ChatConversationDocument conversation,
            Instant now) {
        Map<String, String> members = new LinkedHashMap<>();

        List<UserChallengeEntity> activeParticipants = userChallengeRepository
                .findByChallenge_IdAndStatus(challenge.getId(), Enums.UserChallengeStatus.ACTIVE);
        List<UserChallengeEntity> doneParticipants = userChallengeRepository
                .findByChallenge_IdAndStatus(challenge.getId(), Enums.UserChallengeStatus.DONE);

        for (UserChallengeEntity participant : activeParticipants) {
            members.put(participant.getUser().getId().toString(), "MEMBER");
        }
        for (UserChallengeEntity participant : doneParticipants) {
            members.put(participant.getUser().getId().toString(), "MEMBER");
        }

        members.put(challenge.getCreator().getId().toString(), "OWNER");

        for (Map.Entry<String, String> entry : members.entrySet()) {
            upsertMembership(conversation.getId(), entry.getKey(), entry.getValue(), now);
        }
    }

    private void ensureCanViewChallengeChannels(ChallengeEntity challenge, String currentUserId, String currentRole) {
        if (isModeratorOrAdmin(currentRole)) {
            return;
        }

        if (challenge.getCreator().getId().toString().equals(currentUserId)) {
            return;
        }

        UUID userId = parseUuid(currentUserId, "currentUserId");
        boolean isParticipant = userChallengeRepository
                .findByUser_IdAndChallenge_IdAndStatus(userId, challenge.getId(), Enums.UserChallengeStatus.ACTIVE)
                .isPresent()
                || userChallengeRepository
                        .findByUser_IdAndChallenge_IdAndStatus(userId, challenge.getId(),
                                Enums.UserChallengeStatus.DONE)
                        .isPresent();
        if (!isParticipant) {
            throw new ApiException(ErrorCode.CHAT_MEMBER_REQUIRED,
                    "Ban khong phai thanh vien cua challenge chat");
        }
    }

    private void ensureCanCreateChallengeChannel(ChallengeEntity challenge, String currentUserId, String currentRole) {
        if (isModeratorOrAdmin(currentRole)) {
            return;
        }
        if ("CREATOR".equals(currentRole) && challenge.getCreator().getId().toString().equals(currentUserId)) {
            return;
        }
        throw new ApiException(ErrorCode.FORBIDDEN, "Ban khong co quyen tao channel");
    }

    private void ensureEditWindow(ChatMessageDocument message) {
        Instant createdAt = message.getCreatedAt();
        if (createdAt == null || Instant.now().isAfter(createdAt.plus(MESSAGE_EDIT_WINDOW))) {
            throw new ApiException(ErrorCode.CHAT_MESSAGE_EDIT_WINDOW_EXPIRED,
                    "Da qua thoi gian cho phep sua hoac xoa tin nhan");
        }
    }

    private boolean isModeratorOrAdmin(String role) {
        return "ADMIN".equals(role) || "MODERATOR".equals(role);
    }

    private ChatConversationSummaryResponse toConversationSummary(ChatConversationDocument conversation,
            ChatMembershipDocument myMembership,
            Map<String, List<ChatMembershipDocument>> membershipsByConversationId,
            Map<String, UserEntity> counterpartsById,
            Map<String, ChallengeEntity> challengesById,
            String currentUserId) {
        if (myMembership == null) {
            return null;
        }

        ChatConversationSummaryResponse.CounterpartView counterpart = null;
        if (TYPE_DIRECT.equals(conversation.getType())) {
            Optional<String> counterpartId = resolveCounterpartId(
                    conversation.getId(),
                    currentUserId,
                    membershipsByConversationId.getOrDefault(conversation.getId(), List.of()));
            if (counterpartId.isPresent()) {
                UserEntity user = counterpartsById.get(counterpartId.get());
                if (user != null) {
                    counterpart = new ChatConversationSummaryResponse.CounterpartView(
                            user.getId().toString(),
                            user.getUsername(),
                            user.getAvatarUrl());
                }
            }
        }

        ChatConversationSummaryResponse.ChallengeView challengeView = null;
        ChatConversationSummaryResponse.ChannelView channelView = null;
        if (TYPE_CHANNEL.equals(conversation.getType())) {
            ChallengeEntity challenge = challengesById.get(conversation.getChallengeId());
            if (challenge != null) {
                challengeView = new ChatConversationSummaryResponse.ChallengeView(
                        challenge.getId().toString(),
                        challenge.getTitle());
            }
            channelView = new ChatConversationSummaryResponse.ChannelView(
                    conversation.getChannelKey(),
                    conversation.getChannelName(),
                    conversation.isDefaultChannel());
        }

        ChatConversationDocument.LastMessageSnapshot lastMessage = conversation.getLastMessage();
        ChatConversationSummaryResponse.LastMessageView lastMessageView = null;
        if (lastMessage != null) {
            lastMessageView = new ChatConversationSummaryResponse.LastMessageView(
                    lastMessage.getMessageId(),
                    lastMessage.getSenderId(),
                    lastMessage.getContentPreview(),
                    lastMessage.getSentAt());
        }

        Instant updatedAt = conversation.getUpdatedAt();
        if (updatedAt == null) {
            updatedAt = conversation.getCreatedAt();
        }

        return new ChatConversationSummaryResponse(
                conversation.getId(),
                conversation.getType(),
                counterpart,
                challengeView,
                channelView,
                lastMessageView,
                myMembership.getUnreadCount(),
                updatedAt);
    }

    private ChatMessageResponse toMessageResponse(ChatMessageDocument message) {
        boolean deleted = message.getDeletedAt() != null;

        String content = deleted ? null : message.getContent();
        List<ChatMessageResponse.AttachmentView> attachments = deleted
                ? List.of()
                : Optional.ofNullable(message.getAttachments())
                        .orElseGet(List::of)
                        .stream()
                        .map(attachment -> new ChatMessageResponse.AttachmentView(
                                attachment.getMediaId(),
                                attachment.getFileUrl(),
                                attachment.getFileType(),
                                attachment.getFileSize()))
                        .toList();

        return new ChatMessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getType(),
                content,
                attachments,
                message.getEditedAt(),
                deleted,
                message.getCreatedAt());
    }

    private void refreshConversationLastMessagePreview(ChatMessageDocument message, boolean deleted) {
        String conversationId = Objects.requireNonNull(message.getConversationId());
        chatConversationRepository.findById(conversationId).ifPresent(conversation -> {
            ChatConversationDocument.LastMessageSnapshot snapshot = conversation.getLastMessage();
            if (snapshot == null || !message.getId().equals(snapshot.getMessageId())) {
                return;
            }

            snapshot.setContentPreview(buildContentPreview(message.getContent(), message.getAttachments(), deleted));
            conversation.setLastMessage(snapshot);
            conversation.setUpdatedAt(Instant.now());
            chatConversationRepository.save(conversation);
        });
    }

    private String buildContentPreview(String content,
            List<ChatMessageDocument.AttachmentSnapshot> attachments,
            boolean deleted) {
        if (deleted) {
            return "Tin nhan da bi xoa";
        }
        if (content != null && !content.isBlank()) {
            return content.length() > 80 ? content.substring(0, 80) : content;
        }
        if (attachments != null && !attachments.isEmpty()) {
            return "[Attachment]";
        }
        return "";
    }

    private String resolveMessageType(String content, List<ChatMessageDocument.AttachmentSnapshot> attachments) {
        if (attachments != null && !attachments.isEmpty() && (content == null || content.isBlank())) {
            return "MEDIA";
        }
        return "TEXT";
    }

    private List<ChatMessageDocument.AttachmentSnapshot> resolveAttachments(
            List<SendChatMessageRequest.AttachmentRequest> attachments,
            String currentUserId) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        List<UUID> mediaIds = attachments.stream()
                .map(SendChatMessageRequest.AttachmentRequest::mediaId)
                .map(mediaId -> parseUuid(mediaId, "attachments.mediaId"))
                .toList();

        List<MediaEntity> mediaEntities = mediaRepository.findAllById(Objects.requireNonNull(mediaIds));
        Map<UUID, MediaEntity> mediaById = mediaEntities.stream()
                .collect(Collectors.toMap(MediaEntity::getId, Function.identity()));

        List<ChatMessageDocument.AttachmentSnapshot> snapshots = new ArrayList<>();
        for (UUID mediaId : mediaIds) {
            MediaEntity media = mediaById.get(mediaId);
            if (media == null) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Khong tim thay media attachment");
            }
            if (media.getStatus() != Enums.MediaStatus.CONFIRMED) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Media attachment chua duoc confirm");
            }
            if (!media.getUser().getId().toString().equals(currentUserId)) {
                throw new ApiException(ErrorCode.FORBIDDEN, "Ban khong co quyen su dung media attachment nay");
            }

            ChatMessageDocument.AttachmentSnapshot snapshot = new ChatMessageDocument.AttachmentSnapshot();
            snapshot.setMediaId(media.getId().toString());
            snapshot.setFileUrl(media.getFileUrl());
            snapshot.setFileType(media.getFileType());
            snapshot.setFileSize(media.getFileSize());
            snapshots.add(snapshot);
        }

        return snapshots;
    }

    private Optional<String> resolveCounterpartId(String conversationId,
            String currentUserId,
            List<ChatMembershipDocument> memberships) {
        if (memberships == null || memberships.isEmpty()) {
            return Optional.empty();
        }
        return memberships.stream()
                .map(ChatMembershipDocument::getUserId)
                .filter(userId -> !currentUserId.equals(userId))
                .findFirst();
    }

    private Map<String, UserEntity> findUsersById(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> ids = userIds.stream()
                .map(this::tryParseUuid)
                .flatMap(Optional::stream)
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(user -> user.getId().toString(), Function.identity()));
    }

    private Map<String, ChallengeEntity> findChallengesById(Collection<String> challengeIds) {
        if (challengeIds == null || challengeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> ids = challengeIds.stream()
                .map(this::tryParseUuid)
                .flatMap(Optional::stream)
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        return challengeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(challenge -> challenge.getId().toString(), Function.identity()));
    }

    private boolean filterByType(ChatConversationDocument conversation, String type) {
        return type == null || type.equals(conversation.getType());
    }

    private boolean filterByChallenge(ChatConversationDocument conversation, UUID challengeId) {
        if (challengeId == null) {
            return true;
        }
        return TYPE_CHANNEL.equals(conversation.getType())
                && challengeId.toString().equals(conversation.getChallengeId());
    }

    private boolean filterByKeyword(ChatConversationSummaryResponse item, String keyword) {
        if (keyword == null) {
            return true;
        }
        if (TYPE_DIRECT.equals(item.type()) && item.counterpart() != null) {
            String username = item.counterpart().username();
            return username != null && username.toLowerCase(Locale.ROOT).contains(keyword);
        }
        if (TYPE_CHANNEL.equals(item.type()) && item.channel() != null) {
            String channelName = item.channel().name();
            return channelName != null && channelName.toLowerCase(Locale.ROOT).contains(keyword);
        }
        return false;
    }

    private String normalizeConversationType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if (!TYPE_DIRECT.equals(normalized) && !TYPE_CHANNEL.equals(normalized)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Loai conversation khong hop le");
        }
        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String participantsHash(UUID userA, UUID userB) {
        List<String> ids = List.of(userA.toString(), userB.toString()).stream()
                .sorted()
                .toList();
        return ids.get(0) + ":" + ids.get(1);
    }

    private UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, field + " khong hop le");
        }
    }

    private Optional<UUID> tryParseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private UUID parseOptionalUuid(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseUuid(value, field);
    }

    private int normalizePage(int page) {
        return page < 1 ? 1 : page;
    }

    private int normalizeSize(int size, int defaultValue, int maxValue) {
        if (size < 1) {
            return defaultValue;
        }
        return Math.min(size, maxValue);
    }
}
