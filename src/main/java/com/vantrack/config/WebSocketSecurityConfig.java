package com.vantrack.config;

import com.vantrack.tracking.web.websocket.TripSubscriptionAuthorizationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;


@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig  {

    @Bean("csrfChannelInterceptor")
    public ChannelInterceptor noopCsrfChannelInterceptor() {
        return new ChannelInterceptor() {};
    }

    @Bean
    AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages, TripSubscriptionAuthorizationManager tripSubscriptionAuthorizationManager) {
        return messages
                .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.DISCONNECT,
                        SimpMessageType.HEARTBEAT, SimpMessageType.UNSUBSCRIBE).permitAll()
                .simpSubscribeDestMatchers("/topic/trips/**").access(tripSubscriptionAuthorizationManager)
                .anyMessage().denyAll()
                .build();
    }

}
