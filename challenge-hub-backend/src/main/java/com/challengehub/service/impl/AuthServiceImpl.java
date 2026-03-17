package com.challengehub.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.challengehub.dto.request.LoginRequest;
import com.challengehub.dto.request.RegisterRequest;
import com.challengehub.dto.response.AuthResponse;
import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.UserEntity;
import com.challengehub.exception.ApiException;
import com.challengehub.repository.postgres.UserRepository;
import com.challengehub.security.JwtProperties;
import com.challengehub.security.JwtTokenProvider;
import com.challengehub.service.AuthService;

import io.jsonwebtoken.Claims;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String RT_KEY_PREFIX = "rt:";
    private static final String RT_FAMILY_PREFIX = "rt_family:";
    private static final String RT_FAMILY_TOKENS_PREFIX = "rt_family_tokens:";
    private static final String RT_USER_FAMILIES_PREFIX = "rt_user_families:";
    private static final String AT_BLACKLIST_PREFIX = "blacklist:at:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    public AuthServiceImpl(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            JwtProperties jwtProperties,
            StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public AuthResult register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(com.challengehub.exception.ErrorCode.VALIDATION_DUPLICATE_EMAIL, "Email da ton tai");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(com.challengehub.exception.ErrorCode.VALIDATION_DUPLICATE_USERNAME,
                    "Username da ton tai");
        }

        UserEntity user = new UserEntity();
        user.setEmail(request.email().trim().toLowerCase());
        user.setUsername(request.username().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Enums.UserRole.USER);
        user.setStatus(Enums.UserStatus.ACTIVE);
        user = userRepository.save(user);

        AuthResult issuedTokens = issueTokens(user, true);
        return new AuthResult(toRegisterResponse(user), issuedTokens.refreshToken());
    }

    @Override
    public AuthResult login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.AUTH_INVALID_CREDENTIALS,
                        "Thong tin dang nhap khong dung"));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new ApiException(com.challengehub.exception.ErrorCode.AUTH_ACCOUNT_LOCKED,
                    "Tai khoan dang bi khoa tam thoi");
        }
        if (user.getStatus() == Enums.UserStatus.BANNED) {
            throw new ApiException(com.challengehub.exception.ErrorCode.AUTH_ACCOUNT_BANNED, "Tai khoan da bi cam");
        }
        if (user.getStatus() == Enums.UserStatus.SUSPENDED) {
            throw new ApiException(com.challengehub.exception.ErrorCode.AUTH_ACCOUNT_SUSPENDED,
                    "Tai khoan dang tam khoa");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            int failed = Optional.ofNullable(user.getLoginFailedCount()).orElse(0) + 1;
            user.setLoginFailedCount(failed);
            if (failed >= 5) {
                user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(15)));
            }
            userRepository.save(user);
            throw new ApiException(com.challengehub.exception.ErrorCode.AUTH_INVALID_CREDENTIALS,
                    "Thong tin dang nhap khong dung");
        }

        user.setLoginFailedCount(0);
        user.setLockedUntil(null);
        user = userRepository.save(user);

        return issueTokens(user, true);
    }

    @Override
    public AuthResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiException(com.challengehub.exception.ErrorCode.AUTH_TOKEN_INVALID,
                    "Refresh token khong hop le");
        }

        String tokenHash = sha256(refreshToken);
        String tokenKey = RT_KEY_PREFIX + tokenHash;
        Object userIdObj = redisTemplate.opsForHash().get(tokenKey, "user_id");
        Object familyIdObj = redisTemplate.opsForHash().get(tokenKey, "family_id");
        Object revokedObj = redisTemplate.opsForHash().get(tokenKey, "revoked");

        if (userIdObj == null || familyIdObj == null || revokedObj == null) {
            throw new ApiException(com.challengehub.exception.ErrorCode.AUTH_TOKEN_INVALID,
                    "Refresh token khong hop le");
        }

        String familyId = String.valueOf(familyIdObj);
        String familyKey = RT_FAMILY_PREFIX + familyId;
        Object blockedObj = redisTemplate.opsForHash().get(familyKey, "blocked");

        if ("1".equals(String.valueOf(revokedObj)) || "1".equals(String.valueOf(blockedObj))) {
            blockFamily(familyId, String.valueOf(userIdObj));
            throw new ApiException(com.challengehub.exception.ErrorCode.AUTH_REFRESH_REPLAY,
                    "Phat hien refresh replay");
        }

        redisTemplate.opsForHash().put(tokenKey, "revoked", "1");

        UserEntity user = userRepository.findById(UUID.fromString(String.valueOf(userIdObj)))
                .orElseThrow(() -> new ApiException(com.challengehub.exception.ErrorCode.AUTH_TOKEN_INVALID,
                        "Refresh token khong hop le"));

        String newRefreshToken = createRefreshToken();
        storeRefreshToken(newRefreshToken, user.getId().toString(), familyId);

        String accessToken = createAccessToken(user);
        return new AuthResult(toRefreshResponse(accessToken), newRefreshToken);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            String raw = accessToken.substring(7);
            if (jwtTokenProvider.isValid(raw)) {
                Claims claims = jwtTokenProvider.parseClaims(raw);
                String jti = String.valueOf(claims.get("jti"));
                Instant exp = claims.getExpiration().toInstant();
                long ttl = Duration.between(Instant.now(), exp).getSeconds();
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(AT_BLACKLIST_PREFIX + jti, "1", Duration.ofSeconds(ttl));
                }
            }
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            String tokenHash = sha256(refreshToken);
            String tokenKey = RT_KEY_PREFIX + tokenHash;
            Object familyIdObj = redisTemplate.opsForHash().get(tokenKey, "family_id");
            Object userIdObj = redisTemplate.opsForHash().get(tokenKey, "user_id");
            if (familyIdObj != null && userIdObj != null) {
                blockFamily(String.valueOf(familyIdObj), String.valueOf(userIdObj));
            }
        }
    }

    private AuthResult issueTokens(UserEntity user, boolean newFamily) {
        String familyId = newFamily ? UUID.randomUUID().toString() : null;
        String refreshToken = createRefreshToken();
        String useFamilyId = familyId == null ? UUID.randomUUID().toString() : familyId;
        storeRefreshToken(refreshToken, user.getId().toString(), useFamilyId);
        String accessToken = createAccessToken(user);
        return new AuthResult(toLoginResponse(user, accessToken), refreshToken);
    }

    private String createAccessToken(UserEntity user) {
        String jti = UUID.randomUUID().toString();
        return jwtTokenProvider.generateAccessToken(
                user.getId().toString(),
                Map.of("role", user.getRole().name(), "jti", jti));
    }

    private void storeRefreshToken(String rawToken, String userId, String familyId) {
        String tokenHash = sha256(rawToken);
        String tokenKey = RT_KEY_PREFIX + tokenHash;
        String familyKey = RT_FAMILY_PREFIX + familyId;
        String familyTokensKey = RT_FAMILY_TOKENS_PREFIX + familyId;
        String userFamiliesKey = RT_USER_FAMILIES_PREFIX + userId;
        Duration ttl = Duration.ofMillis(jwtProperties.getRefreshTokenExpirationMs());

        redisTemplate.opsForHash().put(tokenKey, "user_id", userId);
        redisTemplate.opsForHash().put(tokenKey, "family_id", familyId);
        redisTemplate.opsForHash().put(tokenKey, "revoked", "0");
        redisTemplate.expire(tokenKey, ttl);

        redisTemplate.opsForHash().put(familyKey, "user_id", userId);
        redisTemplate.opsForHash().put(familyKey, "blocked", "0");
        redisTemplate.expire(familyKey, ttl);

        redisTemplate.opsForSet().add(familyTokensKey, tokenHash);
        redisTemplate.expire(familyTokensKey, ttl);

        redisTemplate.opsForSet().add(userFamiliesKey, familyId);
        redisTemplate.expire(userFamiliesKey, ttl);
    }

    private void blockFamily(String familyId, String userId) {
        String familyKey = RT_FAMILY_PREFIX + familyId;
        String familyTokensKey = RT_FAMILY_TOKENS_PREFIX + familyId;
        String userFamiliesKey = RT_USER_FAMILIES_PREFIX + userId;

        redisTemplate.opsForHash().put(familyKey, "blocked", "1");
        var hashes = redisTemplate.opsForSet().members(familyTokensKey);
        if (hashes != null) {
            for (String hash : hashes) {
                redisTemplate.delete(RT_KEY_PREFIX + hash);
            }
        }
        redisTemplate.opsForSet().remove(userFamiliesKey, familyId);
    }

    private AuthResponse toLoginResponse(UserEntity user, String accessToken) {
        return AuthResponse.login(
                accessToken,
                "Bearer",
                jwtProperties.getAccessTokenExpirationMs() / 1000,
                new AuthResponse.UserView(
                        user.getId().toString(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getAvatarUrl(),
                        user.getDisplayName()));
    }

    private AuthResponse toRefreshResponse(String accessToken) {
        return AuthResponse.refresh(
                accessToken,
                "Bearer",
                jwtProperties.getAccessTokenExpirationMs() / 1000);
    }

    private AuthResponse toRegisterResponse(UserEntity user) {
        Instant createdAt = user.getCreatedAt() != null ? user.getCreatedAt() : Instant.now();
        return AuthResponse.register(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                createdAt);
    }

    private String createRefreshToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ApiException(com.challengehub.exception.ErrorCode.INTERNAL_ERROR, "Khong the tao token hash");
        }
    }
}
