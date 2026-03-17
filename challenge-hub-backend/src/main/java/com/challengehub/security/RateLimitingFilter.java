package com.challengehub.security;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.challengehub.config.RateLimitProperties;
import com.challengehub.dto.response.ApiResponse;
import com.challengehub.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final Pattern CHAT_MESSAGE_SEND_PATH = Pattern
            .compile("^/api/v1/chat/conversations/[^/]+/messages$");

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(StringRedisTemplate redisTemplate,
            RateLimitProperties rateLimitProperties,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("null")
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LimitRule limitRule = resolveLimitRule(request, authentication);
        String subjectKey = resolveSubjectKey(request, authentication);
        long epochMinute = Instant.now().getEpochSecond() / 60;
        String redisKey = "rate_limit:" + limitRule.routeKey() + ":" + subjectKey + ":" + epochMinute;

        try {
            Long current = redisTemplate.opsForValue().increment(redisKey);
            if (current != null && current == 1L) {
                redisTemplate.expire(redisKey, Duration.ofSeconds(70));
            }
            if (current != null && current > limitRule.limit()) {
                writeRateLimited(response, limitRule.errorCode());
                return;
            }
        } catch (Exception ex) {
            LOGGER.warn("Rate limiting fallback (allow request) for path={}", request.getRequestURI(), ex);
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return request.getRequestURI().startsWith("/actuator/");
    }

    private LimitRule resolveLimitRule(HttpServletRequest request, Authentication authentication) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("POST".equalsIgnoreCase(method) && "/api/v1/auth/login".equals(path)) {
            return new LimitRule("auth_login", rateLimitProperties.getLoginPerMinute(), ErrorCode.RATE_LIMITED);
        }
        if ("POST".equalsIgnoreCase(method) && "/api/v1/auth/register".equals(path)) {
            return new LimitRule("auth_register", rateLimitProperties.getRegisterPerMinute(), ErrorCode.RATE_LIMITED);
        }
        if ("POST".equalsIgnoreCase(method) && "/api/v1/media/upload-url".equals(path)) {
            return new LimitRule("media_upload_url", rateLimitProperties.getUploadUrlPerMinute(),
                    ErrorCode.RATE_LIMITED);
        }
        if ("POST".equalsIgnoreCase(method) && CHAT_MESSAGE_SEND_PATH.matcher(path).matches()) {
            return new LimitRule("chat_send_message", rateLimitProperties.getMessagingSendPerMinute(),
                    ErrorCode.CHAT_RATE_LIMITED);
        }

        if (isAuthenticated(authentication)) {
            return new LimitRule("authenticated", rateLimitProperties.getAuthenticatedPerMinute(),
                    ErrorCode.RATE_LIMITED);
        }
        return new LimitRule("public", rateLimitProperties.getPublicPerMinute(), ErrorCode.RATE_LIMITED);
    }

    private String resolveSubjectKey(HttpServletRequest request, Authentication authentication) {
        if (isAuthenticated(authentication) && authentication.getPrincipal() != null) {
            return "user:" + authentication.getPrincipal();
        }
        return "ip:" + resolveClientIp(request);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            return commaIndex >= 0 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor.trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimited(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> payload = ApiResponse.error(
                errorCode.name(),
                errorCode.getMessage());
        objectMapper.writeValue(response.getOutputStream(), payload);
    }

    private record LimitRule(String routeKey, int limit, ErrorCode errorCode) {
    }
}
