package com.vantrack.tracking.web.websocket;

import com.vantrack.tracking.authorization.TripAccessPolicy;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.access.intercept.MessageAuthorizationContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

@Component
public class TripSubscriptionAuthorizationManager implements AuthorizationManager<MessageAuthorizationContext<?>> {


    private final TripAccessPolicy tripAccessPolicy;

    public TripSubscriptionAuthorizationManager(TripAccessPolicy tripAccessPolicy) {

        this.tripAccessPolicy = tripAccessPolicy;
    }

    @Override
    public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, MessageAuthorizationContext<?> object) {

        Authentication auth = authentication.get();

        if(auth == null) {
            return new AuthorizationDecision(false);
        }

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)
                || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        String userIdClaim = jwtAuth.getToken().getClaimAsString("userId");

        if (userIdClaim == null || userIdClaim.isBlank()) {
            return new AuthorizationDecision(false);
        }

        UUID userId;

        try {
            userId = UUID.fromString(userIdClaim);
        } catch (IllegalArgumentException e) {
            return new AuthorizationDecision(false);
        }

        Message<?> message = object.getMessage();

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if(accessor == null) {
            return new AuthorizationDecision(false);
        }

        String destination = accessor.getDestination();

        if (destination == null) {
            return new AuthorizationDecision(false);
        }

        String prefix = "/topic/trips/";

        if(!destination.startsWith(prefix)) {
            return new AuthorizationDecision(false);
        }

        String tripIdValue =
                destination.substring(prefix.length());

        if (tripIdValue.isBlank() || tripIdValue.contains("/")) {
            return new AuthorizationDecision(false);
        }

        UUID tripId;

        try {
            tripId = UUID.fromString(tripIdValue);
        } catch (IllegalArgumentException e) {
            return new AuthorizationDecision(false);
        }

        boolean canAccess = tripAccessPolicy.canAccess(userId, tripId);

        return new AuthorizationDecision(canAccess);
    }
}
