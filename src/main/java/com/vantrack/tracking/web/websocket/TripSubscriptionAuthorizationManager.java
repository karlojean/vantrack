package com.vantrack.tracking.web.websocket;

import com.vantrack.tracking.authorization.TripAccessPolicy;
import com.vantrack.tracking.UserRouteRepository;
import com.vantrack.trips.TripRepository;
import com.vantrack.users.UserRepository;
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

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class TripSubscriptionAuthorizationManager implements AuthorizationManager<MessageAuthorizationContext<?>> {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final UserRouteRepository userRouteRepository;
    private final TripAccessPolicy tripAccessPolicy;

    public TripSubscriptionAuthorizationManager(UserRepository userRepository, TripRepository tripRepository, UserRouteRepository userRouteRepository, TripAccessPolicy tripAccessPolicy) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.userRouteRepository = userRouteRepository;
        this.tripAccessPolicy = tripAccessPolicy;
    }

    @Override
    public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, MessageAuthorizationContext<?> object) {

        JwtAuthenticationToken auth = (JwtAuthenticationToken) authentication.get();
        var jwt = auth.getToken();

        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getClaimAsString("userId")));

        Message<?> message = object.getMessage();

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        String destination = accessor.getDestination();

        String tripIdValue =
                destination.substring("/topic/trips/".length());

        UUID tripId = UUID.fromString(tripIdValue);

        boolean canAccess = tripAccessPolicy.canAccess(userId, tripId);

        return new AuthorizationDecision(canAccess);
    }
}
