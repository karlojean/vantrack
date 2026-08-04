package com.vantrack.tracking;

import com.vantrack.routes.Route;
import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.tracking.web.dto.SendLocationRequest;
import com.vantrack.trips.Trip;
import com.vantrack.trips.TripRepository;
import com.vantrack.trips.TripStatus;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /trackings - send location")
class SendLocationUseCaseIT extends IntegrationTestSupport {

    private static final BigDecimal LATITUDE = new BigDecimal("-23.550520");
    private static final BigDecimal LONGITUDE = new BigDecimal("-46.633308");

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private TripLocationRepository tripLocationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("the driver records a location on their own active trip")
    void shouldSaveLocationForOwnActiveTrip() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        Trip trip = givenActiveTrip(route);

        OffsetDateTime beforeCall = OffsetDateTime.now();

        mockMvc.perform(post("/trackings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf(new SendLocationRequest(trip.getId(), LATITUDE, LONGITUDE))))
                .andExpect(status().isCreated());

        assertThat(tripLocationRepository.findAll()).singleElement().satisfies(location -> {
            assertThat(location.getLatitude()).isEqualByComparingTo(LATITUDE);
            assertThat(location.getLongitude()).isEqualByComparingTo(LONGITUDE);
            assertThat(location.getCreatedAt()).isNotNull().isAfterOrEqualTo(beforeCall);
        });

        assertThat(tripIdsOfLocations()).containsExactly(trip.getId());
    }

    @Test
    @DisplayName("successive locations pile up on the same trip")
    void shouldAppendEveryLocationSentForTheTrip() throws Exception {
        User driver = data.driver();
        Trip trip = givenActiveTrip(data.route(data.van(driver)));

        sendOk(driver, new SendLocationRequest(trip.getId(), LATITUDE, LONGITUDE));
        sendOk(driver, new SendLocationRequest(trip.getId(), new BigDecimal("-23.551900"), new BigDecimal("-46.634500")));

        assertThat(tripLocationRepository.count()).isEqualTo(2);
        assertThat(tripIdsOfLocations()).containsExactly(trip.getId(), trip.getId());
    }

    @Test
    @DisplayName("a location goes only to the trip it names")
    void shouldSaveLocationOnlyOnTheGivenTrip() throws Exception {
        User driver = data.driver();
        Trip target = givenActiveTrip(data.route(data.van(driver)));
        Trip other = givenActiveTrip(data.route(data.van(driver)));

        sendOk(driver, new SendLocationRequest(target.getId(), LATITUDE, LONGITUDE));

        assertThat(tripIdsOfLocations()).containsExactly(target.getId());
        assertThat(tripIdsOfLocations()).doesNotContain(other.getId());
    }

    @Test
    @DisplayName("the given trip must exist")
    void shouldRejectWhenTripDoesNotExist() throws Exception {
        User driver = data.driver();

        mockMvc.perform(post("/trackings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf(new SendLocationRequest(UUID.randomUUID(), LATITUDE, LONGITUDE))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Viagem não encontrado"));

        assertThat(tripLocationRepository.count()).isZero();
    }

    @Test
    @DisplayName("request without a token is rejected")
    void shouldRejectWhenNotAuthenticated() throws Exception {
        Trip trip = givenActiveTrip(data.route(data.van(data.driver())));

        mockMvc.perform(post("/trackings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf(new SendLocationRequest(trip.getId(), LATITUDE, LONGITUDE))))
                .andExpect(status().isUnauthorized());

        assertThat(tripLocationRepository.count()).isZero();
    }

    @Test
    @DisplayName("a token whose user no longer exists is rejected")
    void shouldRejectWhenAuthenticatedUserWasRemoved() throws Exception {
        User driver = data.driver();
        Trip trip = givenActiveTrip(data.route(data.van(data.driver())));

        String bearer = data.bearerFor(driver);
        userRepository.delete(driver);

        mockMvc.perform(post("/trackings")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf(new SendLocationRequest(trip.getId(), LATITUDE, LONGITUDE))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Erro ao buscar usuário"));

        assertThat(tripLocationRepository.count()).isZero();
    }

    @Test
    @DisplayName("a driver cannot record a location on another driver's trip")
    void shouldRejectWhenDriverDoesNotOwnTheVan() throws Exception {
        User driver = data.driver();
        User anotherDriver = data.driver();
        Trip trip = givenActiveTrip(data.route(data.van(anotherDriver)));

        mockMvc.perform(post("/trackings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf(new SendLocationRequest(trip.getId(), LATITUDE, LONGITUDE))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Não foi possível salvar localização"))
                .andExpect(jsonPath("$.detail").value("Driver so pode salvar localização em viagem que pertence a ele."));

        assertThat(tripLocationRepository.count()).isZero();
    }

    @Test
    @DisplayName("a parent cannot record a location")
    void shouldRejectWhenAuthenticatedAsParent() throws Exception {
        User parent = data.parent();
        Trip trip = givenActiveTrip(data.route(data.van(data.driver())));

        mockMvc.perform(post("/trackings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf(new SendLocationRequest(trip.getId(), LATITUDE, LONGITUDE))))
                .andExpect(status().isForbidden());

        assertThat(tripLocationRepository.count()).isZero();
    }

    @Test
    @DisplayName("an admin cannot record a location: only the van's own driver can")
    void shouldRejectWhenAuthenticatedAsAdmin() throws Exception {
        User admin = data.admin();
        Trip trip = givenActiveTrip(data.route(data.van(data.driver())));

        mockMvc.perform(post("/trackings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf(new SendLocationRequest(trip.getId(), LATITUDE, LONGITUDE))))
                .andExpect(status().isForbidden());

        assertThat(tripLocationRepository.count()).isZero();
    }

    @Test
    @DisplayName("a completed trip no longer accepts locations")
    void shouldRejectWhenTripIsCompleted() throws Exception {
        User driver = data.driver();
        Trip trip = givenCompletedTrip(data.route(data.van(driver)));

        mockMvc.perform(post("/trackings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf(new SendLocationRequest(trip.getId(), LATITUDE, LONGITUDE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Não foi possível salvar localização"))
                .andExpect(jsonPath("$.detail").value("Não e possível salvar localização em viagens finalizadas."));

        assertThat(tripLocationRepository.count()).isZero();
    }

    @Test
    @DisplayName("missing tripId is rejected")
    void shouldRejectWhenTripIdIsMissing() throws Exception {
        User driver = data.driver();

        mockMvc.perform(post("/trackings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf(new SendLocationRequest(null, LATITUDE, LONGITUDE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.tripId").exists());

        assertThat(tripLocationRepository.count()).isZero();
    }

    @Test
    @DisplayName("missing coordinates are rejected")
    void shouldRejectWhenCoordinatesAreMissing() throws Exception {
        User driver = data.driver();
        Trip trip = givenActiveTrip(data.route(data.van(driver)));

        mockMvc.perform(post("/trackings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf(new SendLocationRequest(trip.getId(), null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.latitude").exists())
                .andExpect(jsonPath("$.errors.longitude").exists());

        assertThat(tripLocationRepository.count()).isZero();
    }

    @Test
    @DisplayName("latitude outside [-90, 90] is rejected")
    void shouldRejectWhenLatitudeIsOutOfRange() throws Exception {
        User driver = data.driver();
        Trip trip = givenActiveTrip(data.route(data.van(driver)));

        mockMvc.perform(post("/trackings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf(new SendLocationRequest(trip.getId(), new BigDecimal("90.000001"), LONGITUDE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.latitude").exists());

        assertThat(tripLocationRepository.count()).isZero();
    }

    @Test
    @DisplayName("longitude outside [-180, 180] is rejected")
    void shouldRejectWhenLongitudeIsOutOfRange() throws Exception {
        User driver = data.driver();
        Trip trip = givenActiveTrip(data.route(data.van(driver)));

        mockMvc.perform(post("/trackings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf(new SendLocationRequest(trip.getId(), LATITUDE, new BigDecimal("-180.000001")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.longitude").exists());

        assertThat(tripLocationRepository.count()).isZero();
    }

    @Test
    @DisplayName("a malformed body is rejected")
    void shouldRejectWhenBodyIsMalformed() throws Exception {
        User driver = data.driver();

        mockMvc.perform(post("/trackings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));

        assertThat(tripLocationRepository.count()).isZero();
    }

    private void sendOk(User driver, SendLocationRequest request) throws Exception {
        mockMvc.perform(post("/trackings")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOf(request)))
                .andExpect(status().isCreated());
    }

    private String bodyOf(SendLocationRequest request) {
        return objectMapper.writeValueAsString(request);
    }

    private Trip givenActiveTrip(Route route) {
        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setStatus(TripStatus.ACTIVE);
        trip.setStartedAt(OffsetDateTime.now().minusHours(1));

        return tripRepository.save(trip);
    }

    private Trip givenCompletedTrip(Route route) {
        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setStatus(TripStatus.COMPLETED);
        trip.setStartedAt(OffsetDateTime.now().minusHours(2));
        trip.setEndedAt(OffsetDateTime.now().minusHours(1));

        return tripRepository.save(trip);
    }

    private List<UUID> tripIdsOfLocations() {
        return jdbcTemplate.queryForList(
                "SELECT trip_id FROM trip_locations ORDER BY created_at", UUID.class);
    }
}
