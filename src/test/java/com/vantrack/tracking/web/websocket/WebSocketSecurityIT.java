package com.vantrack.tracking.web.websocket;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.vantrack.routes.Route;
import com.vantrack.routes.UserRoute;
import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.tracking.UserRouteRepository;
import com.vantrack.trips.Trip;
import com.vantrack.trips.TripRepository;
import com.vantrack.trips.TripStatus;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("STOMP WebSocket security")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketSecurityIT extends IntegrationTestSupport {

    private static final String AUTHORIZATION = "Authorization";
    private static final String TOPIC_PREFIX = "/topic/trips/";
    private static final long TIMEOUT_SECONDS = 5;

    @LocalServerPort
    private int port;

    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private UserRouteRepository userRouteRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final List<WebSocketStompClient> clients = new CopyOnWriteArrayList<>();
    private final List<StompSession> sessions = new CopyOnWriteArrayList<>();
    private final List<ThreadPoolTaskScheduler> schedulers = new CopyOnWriteArrayList<>();

    @AfterEach
    void closeStompResources() {
        sessions.stream().filter(StompSession::isConnected).forEach(StompSession::disconnect);
        clients.forEach(WebSocketStompClient::stop);
        schedulers.forEach(ThreadPoolTaskScheduler::stop);
    }

    @Test
    @DisplayName("CONNECT accepts a valid JWT with a UUID userId")
    void shouldConnectWithValidToken() throws Exception {
        StompSession session = connect(data.tokenFor(data.parent()), new RecordingSessionHandler());

        assertThat(session.isConnected()).isTrue();
    }

    @Test
    @DisplayName("CONNECT rejects a missing Authorization header")
    void shouldRejectConnectWithoutToken() throws Exception {
        assertConnectRejected(null);
    }

    @Test
    @DisplayName("CONNECT rejects an empty bearer token")
    void shouldRejectConnectWithEmptyToken() throws Exception {
        assertConnectRejected("");
    }

    @Test
    @DisplayName("CONNECT rejects a malformed token")
    void shouldRejectConnectWithMalformedToken() throws Exception {
        assertConnectRejected("not-a-jwt");
    }

    @Test
    @DisplayName("CONNECT rejects an expired token")
    void shouldRejectConnectWithExpiredToken() throws Exception {
        User parent = data.parent();
        String token = token(jwtEncoder, parent.getId().toString(), Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(60));

        assertConnectRejected(token);
    }

    @Test
    @DisplayName("CONNECT rejects a token signed with another secret")
    void shouldRejectConnectWithInvalidSignature() throws Exception {
        User parent = data.parent();
        var secret = new SecretKeySpec(
                "another-integration-test-secret-key-0123456789".getBytes(), "HmacSHA256");
        JwtEncoder foreignEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret));

        assertConnectRejected(token(foreignEncoder, parent.getId().toString(), Instant.now(),
                Instant.now().plusSeconds(600)));
    }

    @Test
    @DisplayName("CONNECT rejects a token without userId")
    void shouldRejectConnectWithoutUserId() throws Exception {
        assertConnectRejected(token(jwtEncoder, null, Instant.now(), Instant.now().plusSeconds(600)));
    }

    @Test
    @DisplayName("CONNECT rejects a non-UUID userId")
    void shouldRejectConnectWithInvalidUserId() throws Exception {
        assertConnectRejected(token(jwtEncoder, "invalid-user-id", Instant.now(), Instant.now().plusSeconds(600)));
    }

    @Test
    @DisplayName("a parent linked to the route can subscribe to an active trip")
    void shouldAllowLinkedParentToSubscribe() throws Exception {
        User parent = data.parent();
        Route route = data.route(data.van(data.driver()));
        link(parent, route);
        Trip trip = trip(route, TripStatus.ACTIVE);

        assertReceivesMessage(data.tokenFor(parent), TOPIC_PREFIX + trip.getId());
    }

    @Test
    @DisplayName("an admin can subscribe to an active trip")
    void shouldAllowAdminToSubscribe() throws Exception {
        User admin = data.admin();
        Trip trip = trip(data.route(data.van(data.driver())), TripStatus.ACTIVE);

        assertReceivesMessage(data.tokenFor(admin), TOPIC_PREFIX + trip.getId());
    }

    @Test
    @DisplayName("a driver cannot subscribe to a trip")
    void shouldDenyDriverSubscription() throws Exception {
        User driver = data.driver();
        Trip trip = trip(data.route(data.van(driver)), TripStatus.ACTIVE);

        assertSubscriptionRejected(data.tokenFor(driver), TOPIC_PREFIX + trip.getId());
    }

    @Test
    @DisplayName("a parent not linked to the route cannot subscribe")
    void shouldDenyUnlinkedParentSubscription() throws Exception {
        User parent = data.parent();
        Trip trip = trip(data.route(data.van(data.driver())), TripStatus.ACTIVE);

        assertSubscriptionRejected(data.tokenFor(parent), TOPIC_PREFIX + trip.getId());
    }

    @Test
    @DisplayName("a token belonging to a removed user cannot subscribe")
    void shouldDenyRemovedUserSubscription() throws Exception {
        User parent = data.parent();
        String token = data.tokenFor(parent);
        userRepository.delete(parent);
        userRepository.flush();

        assertSubscriptionRejected(token, TOPIC_PREFIX + UUID.randomUUID());
    }

    @Test
    @DisplayName("a completed trip cannot be subscribed to")
    void shouldDenyCompletedTripSubscription() throws Exception {
        User parent = data.parent();
        Route route = data.route(data.van(data.driver()));
        link(parent, route);
        Trip trip = trip(route, TripStatus.COMPLETED);

        assertSubscriptionRejected(data.tokenFor(parent), TOPIC_PREFIX + trip.getId());
    }

    @Test
    @DisplayName("a missing trip cannot be subscribed to")
    void shouldDenyMissingTripSubscription() throws Exception {
        assertSubscriptionRejected(data.tokenFor(data.admin()), TOPIC_PREFIX + UUID.randomUUID());
    }

    @Test
    @DisplayName("a malformed trip UUID is rejected")
    void shouldDenyMalformedTripId() throws Exception {
        assertSubscriptionRejected(data.tokenFor(data.admin()), TOPIC_PREFIX + "not-a-uuid");
    }

    @Test
    @DisplayName("a destination outside the trip topic is rejected")
    void shouldDenyAnotherDestination() throws Exception {
        assertSubscriptionRejected(data.tokenFor(data.admin()), "/topic/another-destination");
    }

    private void assertConnectRejected(String token) throws Exception {
        RecordingSessionHandler handler = new RecordingSessionHandler();
        WebSocketStompClient client = newClient();
        StompHeaders headers = connectHeaders(token);

        client.connectAsync(url(), (WebSocketHttpHeaders) null, headers, handler);

        Throwable failure = handler.failure.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(failure).isNotNull();
        assertThat(rootMessage(failure)).doesNotContain("Jwt", "signature", "expired", "userId");
    }

    private void assertReceivesMessage(String token, String destination) throws Exception {
        RecordingSessionHandler handler = new RecordingSessionHandler();
        StompSession session = connect(token, handler);
        CompletableFuture<String> received = new CompletableFuture<>();
        session.subscribe(destination, stringFrame(received));

        ScheduledFuture<?> publisher = schedulers.getLast().scheduleAtFixedRate(
                () -> messagingTemplate.convertAndSend(destination, "location-update"),
                Duration.ofMillis(50));
        try {
            CompletableFuture.anyOf(received, handler.failure).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(handler.failure)
                    .as("the authorized subscription must not receive an ERROR frame or transport failure")
                    .isNotDone();
            assertThat(received.getNow(null)).isEqualTo("location-update");
        } finally {
            publisher.cancel(false);
        }
    }

    private void assertSubscriptionRejected(String token, String destination) throws Exception {
        RecordingSessionHandler handler = new RecordingSessionHandler();
        StompSession session = connect(token, handler);

        session.setAutoReceipt(true);
        session.subscribe(destination, stringFrame(new CompletableFuture<>()));

        Throwable failure = handler.failure.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(failure).isNotNull();
    }

    private StompSession connect(String token, RecordingSessionHandler handler) throws Exception {
        StompSession session = newClient()
                .connectAsync(url(), (WebSocketHttpHeaders) null, connectHeaders(token), handler)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        sessions.add(session);
        return session;
    }

    private WebSocketStompClient newClient() {
        var transport = new WebSocketTransport(new StandardWebSocketClient());
        var client = new WebSocketStompClient(new SockJsClient(List.of(transport)));
        client.setMessageConverter(new StringMessageConverter());
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-test-");
        scheduler.initialize();
        schedulers.add(scheduler);
        client.setTaskScheduler(scheduler);
        client.setDefaultHeartbeat(new long[]{0, 0});
        clients.add(client);
        client.start();
        return client;
    }

    private StompHeaders connectHeaders(String token) {
        StompHeaders headers = new StompHeaders();
        if (token != null) {
            headers.set(AUTHORIZATION, "Bearer " + token);
        }
        return headers;
    }

    private String url() {
        return "ws://localhost:" + port + "/ws";
    }

    private StompFrameHandler stringFrame(CompletableFuture<String> received) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.complete((String) payload);
            }
        };
    }

    private String token(JwtEncoder encoder, String userId, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("vantrack-api")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject("websocket-test");
        if (userId != null) {
            claims.claim("userId", userId);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    private Trip trip(Route route, TripStatus status) {
        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setStatus(status);
        trip.setStartedAt(OffsetDateTime.now().minusHours(1));
        if (status == TripStatus.COMPLETED) {
            trip.setEndedAt(OffsetDateTime.now());
        }
        return tripRepository.save(trip);
    }

    private void link(User parent, Route route) {
        UserRoute userRoute = new UserRoute();
        userRoute.setParent(parent);
        userRoute.setRoute(route);
        userRoute.setStudentName("Student");
        userRouteRepository.save(userRoute);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }

    private static final class RecordingSessionHandler extends StompSessionHandlerAdapter {
        private final CompletableFuture<Throwable> failure = new CompletableFuture<>();

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            failure.complete(new IllegalStateException(String.valueOf(payload)));
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            failure.complete(exception);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            failure.complete(exception);
        }
    }
}
