package com.challengehub.security;

import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;

@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final String ACCESS_TOKEN_BLACKLIST_PREFIX = "blacklist:at:";
    private static final String SESSION_TOKEN_KEY = "ws_token";

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    public StompJwtChannelInterceptor(JwtTokenProvider jwtTokenProvider,
            StringRedisTemplate redisTemplate) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);
            if (token == null || token.isBlank() || !jwtTokenProvider.isValid(token)) {
                throw new IllegalArgumentException("AUTH_TOKEN_INVALID");
            }

            Claims claims = jwtTokenProvider.parseClaims(token);
            String jti = claims.get("jti", String.class);
            if (jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(ACCESS_TOKEN_BLACKLIST_PREFIX + jti))) {
                throw new IllegalArgumentException("AUTH_TOKEN_REVOKED");
            }

            String userId = claims.getSubject();
            String role = String.valueOf(claims.get("role"));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            accessor.setUser(authentication);
        }

        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        if (authorization != null && !authorization.isBlank()) {
            return authorization.trim();
        }

        String nativeToken = accessor.getFirstNativeHeader("token");
        if (nativeToken != null && !nativeToken.isBlank()) {
            return nativeToken.trim();
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object fromHandshake = sessionAttributes.get(SESSION_TOKEN_KEY);
            if (fromHandshake instanceof String token && !token.isBlank()) {
                return token.trim();
            }
        }
        return null;
    }
}
