package com.challengehub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.challengehub.security.StompJwtChannelInterceptor;
import com.challengehub.security.WsTokenHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CorsProperties corsProperties;
    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;
    private final WsTokenHandshakeInterceptor wsTokenHandshakeInterceptor;

    public WebSocketConfig(CorsProperties corsProperties,
            StompJwtChannelInterceptor stompJwtChannelInterceptor,
            WsTokenHandshakeInterceptor wsTokenHandshakeInterceptor) {
        this.corsProperties = corsProperties;
        this.stompJwtChannelInterceptor = stompJwtChannelInterceptor;
        this.wsTokenHandshakeInterceptor = wsTokenHandshakeInterceptor;
    }

    @Override
    @SuppressWarnings("null")
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        String[] allowedOrigins = corsProperties.allowedOriginsAsList().toArray(String[]::new);
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .addInterceptors(wsTokenHandshakeInterceptor);

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .addInterceptors(wsTokenHandshakeInterceptor)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(stompJwtChannelInterceptor);
    }
}
