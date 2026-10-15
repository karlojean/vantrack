package com.vantrack.tracking;

import com.vantrack.routes.Route;
import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.tracking.web.dto.SendLocationRequest;
import com.vantrack.trips.Trip;
import com.vantrack.users.User;
import com.vantrack.vans.Van;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DisplayName("Tracking flow")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TrackingFlowIT extends IntegrationTestSupport {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimpleBrokerMessageHandler brokerMessageHandler;

    private static final BigDecimal LATITUDE = new BigDecimal("-23.550520");
    private static final BigDecimal LONGITUDE = new BigDecimal("-46.633308");

    @Test
    @DisplayName("Should deliver location to subscribed parent")
    void shouldDeliverLocationToSubscribedParent() throws Exception {
        User driver = data.driver();
        User parent = data.parent();
        Van van = data.van(driver);
        Route route = data.route(van);

        data.linkParent(parent, route);

        Trip trip = data.trip(route);

        URI url = new URI("ws://localhost:" + port + "/ws");

        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));

        RecordingSessionHandler handler = new RecordingSessionHandler();
        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.set("Authorization", data.bearerFor(parent));

        StompSession stompSession = stompClient.connectAsync(url, null, stompHeaders, handler).get(5, TimeUnit.SECONDS);

        try {

            assertThat(stompSession.isConnected()).isTrue();

            CompletableFuture<byte[]> received = new CompletableFuture<>();

            stompSession.subscribe( "/topic/trips/" + trip.getId(), byteFrame(received));

            for (int attempt = 0; attempt < 25 && !received.isDone() && !handler.failure.isDone(); attempt++) {
                mockMvc.perform(post("/trackings")
                                .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(bodyOf(new SendLocationRequest(trip.getId(), LATITUDE, LONGITUDE))))
                        .andExpect(status().isCreated());

                try {
                    CompletableFuture.anyOf(received, handler.failure).get(200, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ignored) {
                    // Ignore Timeout
                }
            }

            assertThat(handler.failure).isNotDone();
            assertThat(received).isDone();

            String json = new String(received.getNow(null), StandardCharsets.UTF_8);

            assertThat(json)
                    .contains(trip.getId().toString())
                    .contains(LATITUDE.toPlainString())
                    .contains(LONGITUDE.toPlainString());
        } finally {
            stompSession.disconnect();
            stompClient.stop();
        }
    };

    @Test
    @DisplayName("Should deliver the very first location once subscription is confirmed")
    void shouldDeliverFirstLocationAfterSubscriptionIsConfirmed() throws Exception {
        User driver = data.driver();
        User parent = data.parent();
        Van van = data.van(driver);
        Route route = data.route(van);

        data.linkParent(parent, route);

        Trip trip = data.trip(route);

        URI url = new URI("ws://localhost:" + port + "/ws");

        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));

        RecordingSessionHandler handler = new RecordingSessionHandler();
        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.set("Authorization", data.bearerFor(parent));

        StompSession stompSession = stompClient.connectAsync(url, null, stompHeaders, handler).get(5, TimeUnit.SECONDS);

        try {
            assertThat(stompSession.isConnected()).isTrue();

            CompletableFuture<byte[]> received = new CompletableFuture<>();

            String topic = "/topic/trips/" + trip.getId();
            stompSession.subscribe(topic, byteFrame(received));

            awaitBrokerSubscription(topic, handler);

            mockMvc.perform(post("/trackings")
                            .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyOf(new SendLocationRequest(trip.getId(), LATITUDE, LONGITUDE))))
                    .andExpect(status().isCreated());

            byte[] payload = received.get(5, TimeUnit.SECONDS);

            assertThat(handler.failure).isNotDone();

            String json = new String(payload, StandardCharsets.UTF_8);

            assertThat(json)
                    .contains(trip.getId().toString())
                    .contains(LATITUDE.toPlainString())
                    .contains(LONGITUDE.toPlainString());
        } finally {
            stompSession.disconnect();
            stompClient.stop();
        }
    }

    @Test
    @DisplayName("Should notify subscribed parent when trip is finished")
    void shouldNotifySubscribedParentWhenTripIsFinished() throws Exception {
        User driver = data.driver();
        User parent = data.parent();
        Van van = data.van(driver);
        Route route = data.route(van);

        data.linkParent(parent, route);

        Trip trip = data.trip(route);

        URI url = new URI("ws://localhost:" + port + "/ws");

        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));

        RecordingSessionHandler handler = new RecordingSessionHandler();
        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.set("Authorization", data.bearerFor(parent));

        StompSession stompSession = stompClient.connectAsync(url, null, stompHeaders, handler).get(5, TimeUnit.SECONDS);

        try {
            assertThat(stompSession.isConnected()).isTrue();

            CompletableFuture<byte[]> locationReceived = new CompletableFuture<>();
            CompletableFuture<byte[]> finishReceived = new CompletableFuture<>();

            stompSession.subscribe("/topic/trips/" + trip.getId(), new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return byte[].class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    byte[] body = (byte[]) payload;
                    String json = new String(body, StandardCharsets.UTF_8);

                    if (json.contains("TRIP_FINISHED")) {
                        finishReceived.complete(body);
                    } else {
                        locationReceived.complete(body);
                    }
                }
            });

            for (int attempt = 0; attempt < 25 && !locationReceived.isDone() && !handler.failure.isDone(); attempt++) {
                mockMvc.perform(post("/trackings")
                                .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(bodyOf(new SendLocationRequest(trip.getId(), LATITUDE, LONGITUDE))))
                        .andExpect(status().isCreated());

                try {
                    CompletableFuture.anyOf(locationReceived, handler.failure).get(200, TimeUnit.MILLISECONDS);
                } catch (TimeoutException ignored) {
                    // Ignore Timeout
                }
            }

            assertThat(handler.failure).isNotDone();
            assertThat(locationReceived).isDone();

            mockMvc.perform(put("/trips/{id}/finish", trip.getId())
                            .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                    .andExpect(status().isOk());

            byte[] payload = finishReceived.get(5, TimeUnit.SECONDS);

            String json = new String(payload, StandardCharsets.UTF_8);

            assertThat(handler.failure).isNotDone();
            assertThat(json)
                    .contains("\"type\":\"TRIP_FINISHED\"")
                    .contains(trip.getId().toString())
                    .contains("endedAt");
        } finally {
            stompSession.disconnect();
            stompClient.stop();
        }
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

    private StompFrameHandler byteFrame(CompletableFuture<byte[]> received) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.complete((byte[]) payload);
            }
        };
    }

    private void awaitBrokerSubscription(String topic, RecordingSessionHandler handler) throws InterruptedException {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setDestination(topic);
        Message<byte[]> probe = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

        while (System.nanoTime() < deadline) {
            if (handler.failure.isDone()) {
                throw new IllegalStateException("Falha na sessão STOMP antes da inscrição", handler.failure.getNow(null));
            }
            if (!brokerMessageHandler.getSubscriptionRegistry().findSubscriptions(probe).isEmpty()) {
                return;
            }
            Thread.sleep(10);
        }

        throw new IllegalStateException("Inscrição em " + topic + " não foi registrada no broker");
    }

    private String bodyOf(SendLocationRequest request) {
        return objectMapper.writeValueAsString(request);
    }
}
