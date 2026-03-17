package com.challengehub.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.challengehub.dto.request.UpdateUserRoleRequest;
import com.challengehub.dto.request.UpdateUserStatusRequest;
import com.challengehub.dto.response.AdminUserResponse;
import com.challengehub.dto.response.AuditLogResponse;
import com.challengehub.entity.mongodb.AuditLogDocument;
import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.UserEntity;
import com.challengehub.exception.ApiException;
import com.challengehub.exception.ErrorCode;
import com.challengehub.repository.mongodb.AuditLogRepository;
import com.challengehub.repository.postgres.UserRepository;
import com.challengehub.service.AdminService;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private static final String RT_KEY_PREFIX = "rt:";
    private static final String RT_FAMILY_PREFIX = "rt_family:";
    private static final String RT_FAMILY_TOKENS_PREFIX = "rt_family_tokens:";
    private static final String RT_USER_FAMILIES_PREFIX = "rt_user_families:";

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final MongoTemplate mongoTemplate;
    private final StringRedisTemplate redisTemplate;

    public AdminServiceImpl(UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            MongoTemplate mongoTemplate,
            StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.mongoTemplate = mongoTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminUserResponse> getUsers(String role, String status, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(normalizePage(page) - 1, normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<UserEntity> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (role != null && !role.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("role"), parseRole(role)));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), parseStatus(status)));
            }

            if (keyword != null && !keyword.isBlank()) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), normalizedKeyword)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserEntity> users = userRepository.findAll(specification, pageable);
        return toUserPageResult(users.map(this::toAdminUserResponse));
    }

    @Override
    public AdminUserResponse updateUserRole(String userId,
            UpdateUserRoleRequest request,
            String actorId,
            String actorRole,
            String ipAddress,
            String userAgent) {
        UserEntity user = findUser(userId);
        Enums.UserRole oldRole = user.getRole();

        user.setRole(request.role());
        UserEntity savedUser = userRepository.save(user);

        if (oldRole != request.role()) {
            Map<String, Object> oldValue = Map.of("role", oldRole.name());
            Map<String, Object> newValue = Map.of("role", request.role().name());
            saveAuditLog(actorId, actorRole, "CHANGE_USER_ROLE", "USER", savedUser.getId().toString(), oldValue,
                    newValue, ipAddress, userAgent);
        }

        return toAdminUserResponse(savedUser);
    }

    @Override
    public AdminUserResponse updateUserStatus(String userId,
            UpdateUserStatusRequest request,
            String actorId,
            String actorRole,
            String ipAddress,
            String userAgent) {
        validateStatusReason(request);

        UserEntity user = findUser(userId);
        Enums.UserStatus oldStatus = user.getStatus();

        user.setStatus(request.status());
        UserEntity savedUser = userRepository.save(user);

        if (request.status() == Enums.UserStatus.BANNED || request.status() == Enums.UserStatus.SUSPENDED) {
            revokeUserRefreshTokenFamilies(savedUser.getId().toString());
        }

        if (oldStatus != request.status()) {
            Map<String, Object> oldValue = Map.of("status", oldStatus.name());
            Map<String, Object> newValue = new HashMap<>();
            newValue.put("status", request.status().name());
            if (request.reason() != null && !request.reason().isBlank()) {
                newValue.put("reason", request.reason().trim());
            }
            saveAuditLog(actorId, actorRole, resolveStatusAuditAction(request.status()), "USER",
                    savedUser.getId().toString(), oldValue, newValue, ipAddress, userAgent);
        }

        return toAdminUserResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AuditLogResponse> getAuditLogs(String action,
            String actorId,
            String resourceType,
            Instant from,
            Instant to,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(normalizePage(page) - 1, normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "timestamp"));

        boolean hasFilter = hasText(action) || hasText(actorId) || hasText(resourceType) || from != null || to != null;

        if (!hasFilter) {
            Page<AuditLogDocument> auditLogPage = auditLogRepository.findAllByOrderByTimestampDesc(pageable);
            return toAuditLogPageResult(auditLogPage.map(this::toAuditLogResponse));
        }

        Query query = new Query().with(pageable);
        List<Criteria> criteriaList = new ArrayList<>();

        if (hasText(action)) {
            criteriaList.add(Criteria.where("action").is(action.trim()));
        }
        if (hasText(actorId)) {
            criteriaList.add(Criteria.where("actorId").is(actorId.trim()));
        }
        if (hasText(resourceType)) {
            criteriaList.add(Criteria.where("resourceType").is(resourceType.trim()));
        }
        if (from != null && to != null) {
            criteriaList.add(Criteria.where("timestamp").gte(from).lte(to));
        } else if (from != null) {
            criteriaList.add(Criteria.where("timestamp").gte(from));
        } else if (to != null) {
            criteriaList.add(Criteria.where("timestamp").lte(to));
        }

        if (!criteriaList.isEmpty()) {
            Criteria[] criteriaArray = criteriaList.toArray(Criteria[]::new);
            query.addCriteria(new Criteria().andOperator(Objects.requireNonNull(criteriaArray)));
        }

        List<AuditLogDocument> documents = mongoTemplate.find(query, AuditLogDocument.class);
        Query countQuery = Query.of(query).limit(-1).skip(-1);
        long totalElements = mongoTemplate.count(countQuery, AuditLogDocument.class);

        List<AuditLogResponse> items = documents.stream().map(this::toAuditLogResponse).toList();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / normalizedSize);
        return new PageResult<>(items, normalizedPage, normalizedSize, totalElements, totalPages);
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

    private PageResult<AdminUserResponse> toUserPageResult(Page<AdminUserResponse> page) {
        return new PageResult<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private PageResult<AuditLogResponse> toAuditLogPageResult(Page<AuditLogResponse> page) {
        return new PageResult<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private UserEntity findUser(String userId) {
        UUID parsedUserId = parseUuid(userId);
        return userRepository.findById(Objects.requireNonNull(parsedUserId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Khong tim thay nguoi dung"));
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "id khong hop le");
        }
    }

    private Enums.UserRole parseRole(String role) {
        try {
            return Enums.UserRole.valueOf(role.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Gia tri role khong hop le");
        }
    }

    private Enums.UserStatus parseStatus(String status) {
        try {
            return Enums.UserStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Gia tri status khong hop le");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void validateStatusReason(UpdateUserStatusRequest request) {
        boolean requiresReason = request.status() == Enums.UserStatus.BANNED
                || request.status() == Enums.UserStatus.SUSPENDED;
        if (requiresReason && (request.reason() == null || request.reason().isBlank())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "reason la bat buoc khi BANNED/SUSPENDED");
        }
    }

    private void revokeUserRefreshTokenFamilies(String userId) {
        String userFamiliesKey = RT_USER_FAMILIES_PREFIX + userId;
        Set<String> familyIds = redisTemplate.opsForSet().members(userFamiliesKey);
        if (familyIds == null || familyIds.isEmpty()) {
            redisTemplate.delete(userFamiliesKey);
            return;
        }

        for (String familyId : familyIds) {
            String familyKey = RT_FAMILY_PREFIX + familyId;
            String familyTokensKey = RT_FAMILY_TOKENS_PREFIX + familyId;

            redisTemplate.opsForHash().put(familyKey, "blocked", "1");

            Set<String> tokenHashes = redisTemplate.opsForSet().members(familyTokensKey);
            if (tokenHashes != null && !tokenHashes.isEmpty()) {
                for (String tokenHash : tokenHashes) {
                    redisTemplate.delete(RT_KEY_PREFIX + tokenHash);
                }
            }

            redisTemplate.delete(familyTokensKey);
        }

        redisTemplate.delete(userFamiliesKey);
    }

    private String resolveStatusAuditAction(Enums.UserStatus status) {
        return switch (status) {
            case BANNED -> "BAN_USER";
            case SUSPENDED -> "SUSPEND_USER";
            case ACTIVE -> "ACTIVATE_USER";
        };
    }

    private void saveAuditLog(String actorId,
            String actorRole,
            String action,
            String resourceType,
            String resourceId,
            Map<String, Object> oldValue,
            Map<String, Object> newValue,
            String ipAddress,
            String userAgent) {
        AuditLogDocument auditLog = new AuditLogDocument();
        auditLog.setActorId(actorId);
        auditLog.setActorRole(actorRole);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setTimestamp(Instant.now());
        auditLogRepository.save(auditLog);
    }

    private AdminUserResponse toAdminUserResponse(UserEntity user) {
        return new AdminUserResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }

    private AuditLogResponse toAuditLogResponse(AuditLogDocument document) {
        String targetType = document.getResourceType();
        String targetId = document.getResourceId();
        Instant createdAt = document.getTimestamp();

        return new AuditLogResponse(
                document.getId(),
                document.getActorId(),
                document.getActorRole(),
                document.getAction(),
                targetType,
                targetId,
                document.getOldValue(),
                document.getNewValue(),
                document.getIpAddress(),
                document.getUserAgent(),
                createdAt);
    }
}
