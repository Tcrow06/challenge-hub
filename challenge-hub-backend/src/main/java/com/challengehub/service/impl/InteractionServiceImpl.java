package com.challengehub.service.impl;

import com.challengehub.dto.request.CreateCommentRequest;
import com.challengehub.dto.request.ReactSubmissionRequest;
import com.challengehub.dto.response.CommentResponse;
import com.challengehub.dto.response.ReactionResponse;
import com.challengehub.entity.mongodb.CommentDocument;
import com.challengehub.entity.mongodb.ReactionDocument;
import com.challengehub.entity.postgres.SubmissionEntity;
import com.challengehub.entity.postgres.UserEntity;
import com.challengehub.exception.ApiException;
import com.challengehub.repository.mongodb.CommentRepository;
import com.challengehub.repository.mongodb.ReactionRepository;
import com.challengehub.repository.postgres.SubmissionRepository;
import com.challengehub.repository.postgres.UserRepository;
import com.challengehub.service.InteractionService;
import com.challengehub.service.SubmissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class InteractionServiceImpl implements InteractionService {

    private static final List<String> ALLOWED_REACTIONS = List.of("LIKE", "HEART", "FIRE");

    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public InteractionServiceImpl(CommentRepository commentRepository,
                                  ReactionRepository reactionRepository,
                                  SubmissionRepository submissionRepository,
                                  UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public CommentResponse createComment(String submissionId, CreateCommentRequest request, String currentUserId) {
        SubmissionEntity submission = findSubmission(submissionId);
        UserEntity user = findUser(currentUserId);

        CommentDocument doc = new CommentDocument();
        doc.setSubmissionId(submission.getId().toString());
        doc.setUserId(user.getId().toString());
        doc.setUsername(user.getUsername());
        doc.setAvatarUrl(user.getAvatarUrl());
        doc.setContent(request.content().trim());
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        doc = commentRepository.save(doc);

        return toResponse(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionService.PageResult<CommentResponse> getComments(String submissionId, int page, int size) {
        findSubmission(submissionId);
        Pageable pageable = PageRequest.of(normalizePage(page) - 1, normalizeSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommentDocument> comments = commentRepository.findBySubmissionIdOrderByCreatedAtDesc(submissionId, pageable);
        Page<CommentResponse> mapped = comments.map(this::toResponse);
        return new SubmissionService.PageResult<>(
                mapped.getContent(),
                mapped.getNumber() + 1,
                mapped.getSize(),
                mapped.getTotalElements(),
                mapped.getTotalPages()
        );
    }

    @Override
    public ReactionResponse reactSubmission(String submissionId, ReactSubmissionRequest request, String currentUserId) {
        findSubmission(submissionId);
        String type = normalizeReactionType(request.type());

        ReactionDocument existing = reactionRepository.findBySubmissionIdAndUserId(submissionId, currentUserId).orElse(null);
        boolean reacted;
        String currentType;

        if (existing == null) {
            ReactionDocument doc = new ReactionDocument();
            doc.setSubmissionId(submissionId);
            doc.setUserId(currentUserId);
            doc.setType(type);
            doc.setCreatedAt(Instant.now());
            reactionRepository.save(doc);
            reacted = true;
            currentType = type;
        } else if (type.equals(existing.getType())) {
            reactionRepository.delete(existing);
            reacted = false;
            currentType = null;
        } else {
            existing.setType(type);
            reactionRepository.save(existing);
            reacted = true;
            currentType = type;
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        for (String allowedType : ALLOWED_REACTIONS) {
            counts.put(allowedType, reactionRepository.countBySubmissionIdAndType(submissionId, allowedType));
        }

        return new ReactionResponse(reacted, currentType, counts);
    }

    @Override
    public void deleteComment(String commentId, String currentUserId, String currentRole) {
        CommentDocument comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.NOT_FOUND, "Khong tim thay comment"));

        boolean owner = currentUserId.equals(comment.getUserId());
        boolean privileged = "ADMIN".equals(currentRole) || "MODERATOR".equals(currentRole);
        if (!owner && !privileged) {
            throw new ApiException(com.challengehub.exception.ErrorCode.FORBIDDEN, "Ban khong co quyen xoa comment nay");
        }

        commentRepository.delete(comment);
    }

    private SubmissionEntity findSubmission(String submissionId) {
        return submissionRepository.findById(UUID.fromString(submissionId))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.SUBMISSION_NOT_FOUND, "Khong tim thay submission"));
    }

    private UserEntity findUser(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.NOT_FOUND, "Khong tim thay nguoi dung"));
    }

    private String normalizeReactionType(String rawType) {
        String normalized = rawType == null ? "" : rawType.trim().toUpperCase();
        if (!ALLOWED_REACTIONS.contains(normalized)) {
            throw new ApiException(com.challengehub.exception.ErrorCode.VALIDATION_FAILED, "Loai reaction khong hop le");
        }
        return normalized;
    }

    private int normalizePage(int page) {
        return page < 1 ? 1 : page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 50);
    }

    private CommentResponse toResponse(CommentDocument doc) {
        return new CommentResponse(
                doc.getId(),
                doc.getContent(),
                new CommentResponse.UserView(doc.getUserId(), doc.getUsername(), doc.getAvatarUrl()),
                doc.getCreatedAt()
        );
    }
}