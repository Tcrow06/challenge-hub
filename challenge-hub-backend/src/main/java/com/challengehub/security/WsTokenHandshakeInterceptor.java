package com.challengehub.security;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WsTokenHandshakeInterceptor implements HandshakeInterceptor {

    private static final String SESSION_TOKEN_KEY = "ws_token";

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (token != null && !token.isBlank()) {
            attributes.put(SESSION_TOKEN_KEY, token.trim());
        }
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @Nullable Exception exception) {
        // no-op
    }
}
