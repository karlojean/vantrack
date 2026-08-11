package com.vantrack.trips;

import com.vantrack.routes.Route;
import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.tracking.TripLocation;
import com.vantrack.tracking.TripLocationRepository;
import com.vantrack.trips.web.dto.StartTripRequest;
import com.vantrack.users.User;
import com.vantrack.vans.Van;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /trips/start - start trip")
class StartTripUseCaseIT extends IntegrationTestSupport {

    private static final BigDecimal LATITUDE = new BigDecimal("-23.550520");
    private static final BigDecimal LONGITUDE = new BigDecimal("-46.633308");

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private TripLocationRepository tripLocationRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("starting a trip records the first location")
    void shouldStartTripAndRecordFirstLocation() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));

        StartTripRequest request = new StartTripRequest(route.getId(), LATITUDE, LONGITUDE);

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.routeId").value(route.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startedAt").exists())
                .andExpect(jsonPath("$.endedAt").doesNotExist());

        Trip trip = tripRepository.findAll().getFirst();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.ACTIVE);
        assertThat(trip.getStartedAt()).isNotNull();
        assertThat(trip.getEndedAt()).isNull();

        assertThat(tripLocationRepository.findAll()).singleElement().satisfies(location -> {
            assertThat(location.getLatitude()).isEqualByComparingTo(LATITUDE);
            assertThat(location.getLongitude()).isEqualByComparingTo(LONGITUDE);
            // mesmo instante do startedAt: um now() por execução, não dois
            assertThat(location.getCreatedAt()).isEqualTo(trip.getStartedAt());
        });

        assertThat(tripIdOfSingleLocation()).isEqualTo(trip.getId());
    }

    @Test
    @DisplayName("a route with an active trip cannot start another one")
    void shouldRejectWhenRouteAlreadyHasActiveTrip() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));

        StartTripRequest request = new StartTripRequest(route.getId(), LATITUDE, LONGITUDE);
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Não foi possível iniciar viagem"));

        assertThat(tripRepository.count()).isOne();
        assertThat(tripLocationRepository.count()).isOne();
    }

    @Test
    @DisplayName("a completed trip does not block a new one on the same route")
    void shouldStartTripWhenPreviousTripIsCompleted() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        givenCompletedTrip(route);

        StartTripRequest request = new StartTripRequest(route.getId(), LATITUDE, LONGITUDE);

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(tripRepository.count()).isEqualTo(2);
        assertThat(tripLocationRepository.count()).isOne();
    }

    @Test
    @DisplayName("an active trip on another route does not block this one")
    void shouldStartTripWhenActiveTripBelongsToAnotherRoute() throws Exception {
        User driver = data.driver();
        Van van = data.van(driver);
        Route busyRoute = data.route(van);
        Route freeRoute = data.route(van);

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StartTripRequest(busyRoute.getId(), LATITUDE, LONGITUDE))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StartTripRequest(freeRoute.getId(), LATITUDE, LONGITUDE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(freeRoute.getId().toString()));

        assertThat(tripRepository.count()).isEqualTo(2);
        assertThat(tripLocationRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("the given route must exist")
    void shouldRejectWhenRouteDoesNotExist() throws Exception {
        User driver = data.driver();

        StartTripRequest request = new StartTripRequest(UUID.randomUUID(), LATITUDE, LONGITUDE);

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Rota não encontrado"));

        assertThat(tripRepository.count()).isZero();
        assertThat(tripLocationRepository.count()).isZero();
    }

    @Test
    @DisplayName("request without a token is rejected")
    void shouldRejectWhenNotAuthenticated() throws Exception {
        Route route = data.route(data.van(data.driver()));

        StartTripRequest request = new StartTripRequest(route.getId(), LATITUDE, LONGITUDE);

        mockMvc.perform(post("/trips/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        assertThat(tripRepository.count()).isZero();
    }

    @Test
    @DisplayName("a driver cannot start a trip on another driver's route")
    void shouldRejectWhenDriverDoesNotOwnTheVan() throws Exception {
        User driver = data.driver();
        User anotherDriver = data.driver();
        Route route = data.route(data.van(anotherDriver));

        StartTripRequest request = new StartTripRequest(route.getId(), LATITUDE, LONGITUDE);

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Não foi possível iniciar viagem"));

        assertThat(tripRepository.count()).isZero();
        assertThat(tripLocationRepository.count()).isZero();
    }

    @Test
    @DisplayName("a parent cannot start a trip")
    void shouldRejectWhenAuthenticatedAsParent() throws Exception {
        User parent = data.parent();
        Route route = data.route(data.van(data.driver()));

        StartTripRequest request = new StartTripRequest(route.getId(), LATITUDE, LONGITUDE);

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(tripRepository.count()).isZero();
    }

    @Test
    @DisplayName("an admin cannot start a trip: only the van's own driver can")
    void shouldRejectWhenAuthenticatedAsAdmin() throws Exception {
        User admin = data.admin();
        Route route = data.route(data.van(data.driver()));

        StartTripRequest request = new StartTripRequest(route.getId(), LATITUDE, LONGITUDE);

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(tripRepository.count()).isZero();
    }

    @Test
    @DisplayName("missing routeId is rejected")
    void shouldRejectWhenRouteIdIsMissing() throws Exception {
        User driver = data.driver();

        StartTripRequest request = new StartTripRequest(null, LATITUDE, LONGITUDE);

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.routeId").exists());
    }

    @Test
    @DisplayName("missing coordinates are rejected")
    void shouldRejectWhenCoordinatesAreMissing() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));

        StartTripRequest request = new StartTripRequest(route.getId(), null, null);

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.latitude").exists())
                .andExpect(jsonPath("$.errors.longitude").exists());

        assertThat(tripRepository.count()).isZero();
    }

    @Test
    @DisplayName("latitude outside [-90, 90] is rejected")
    void shouldRejectWhenLatitudeIsOutOfRange() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));

        StartTripRequest request = new StartTripRequest(route.getId(), new BigDecimal("90.000001"), LONGITUDE);

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.latitude").exists());

        assertThat(tripRepository.count()).isZero();
    }

    @Test
    @DisplayName("longitude outside [-180, 180] is rejected")
    void shouldRejectWhenLongitudeIsOutOfRange() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));

        StartTripRequest request = new StartTripRequest(route.getId(), LATITUDE, new BigDecimal("-180.000001"));

        mockMvc.perform(post("/trips/start")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.longitude").exists());

        assertThat(tripRepository.count()).isZero();
    }

    @Test
    @DisplayName("")
    void shouldNotAllowStartTwoActiveTripsSimultaneously() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        User driver = data.driver();
        Route route = data.route(data.van(driver));

        StartTripRequest request = new StartTripRequest(route.getId(), LATITUDE, LONGITUDE);

        Future<ResultActions> future1 = executor.submit(() -> {
            latch.await();
            return mockMvc.perform(post("/trips/start")
                    .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        });

        Future<ResultActions> future2 = executor.submit(() -> {
            latch.await();
            return mockMvc.perform(post("/trips/start")
                    .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        });

        latch.countDown();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        int status1 = future1.get().andReturn().getResponse().getStatus();
        int status2 = future2.get().andReturn().getResponse().getStatus();

        System.out.println(status1 + " - " + status2);

        boolean oneSuccessOneFail = (status1 == 201 && status2 == 409) || (status1 == 409 && status2 == 201);
        assertTrue(oneSuccessOneFail, "Uma das requisições deveria ter falhado para evitar duas viagens ativas!");
    }

    private void givenCompletedTrip(Route route) {
        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setStatus(TripStatus.COMPLETED);
        trip.setStartedAt(java.time.OffsetDateTime.now().minusHours(2));
        trip.setEndedAt(java.time.OffsetDateTime.now().minusHours(1));

        tripRepository.save(trip);
    }

    private UUID tripIdOfSingleLocation() {
        return jdbcTemplate.queryForObject("SELECT trip_id FROM trip_locations", UUID.class);
    }
}
