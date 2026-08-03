package com.vantrack.trips;

import com.vantrack.routes.Route;
import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.users.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PUT /trips/{id}/finish - finish trip")
class FinishTripUseCaseIT extends IntegrationTestSupport {

    @Autowired
    private TripRepository tripRepository;

    @Test
    @DisplayName("finishing a trip completes it and stamps endedAt")
    void shouldFinishTripAndSetEndedAt() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        Trip trip = givenActiveTrip(route);

        OffsetDateTime beforeCall = OffsetDateTime.now();

        mockMvc.perform(put("/trips/{id}/finish", trip.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(trip.getId().toString()))
                .andExpect(jsonPath("$.routeId").value(route.getId().toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.startedAt").exists())
                .andExpect(jsonPath("$.endedAt").exists());

        Trip finished = tripRepository.findById(trip.getId()).orElseThrow();
        assertThat(finished.getStatus()).isEqualTo(TripStatus.COMPLETED);
        // o banco guarda microssegundos, o objeto em memória guarda nanos
        assertThat(finished.getStartedAt()).isCloseTo(trip.getStartedAt(), within(1, ChronoUnit.MICROS));
        assertThat(finished.getEndedAt())
                .isNotNull()
                .isAfterOrEqualTo(beforeCall)
                .isAfter(finished.getStartedAt());
    }

    @Test
    @DisplayName("finishing one trip does not touch the driver's other trips")
    void shouldFinishOnlyTheRequestedTrip() throws Exception {
        User driver = data.driver();
        Trip target = givenActiveTrip(data.route(data.van(driver)));
        Trip untouched = givenActiveTrip(data.route(data.van(driver)));

        mockMvc.perform(put("/trips/{id}/finish", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isOk());

        Trip stillActive = tripRepository.findById(untouched.getId()).orElseThrow();
        assertThat(stillActive.getStatus()).isEqualTo(TripStatus.ACTIVE);
        assertThat(stillActive.getEndedAt()).isNull();
    }

    @Test
    @DisplayName("the given trip must exist")
    void shouldRejectWhenTripDoesNotExist() throws Exception {
        User driver = data.driver();

        mockMvc.perform(put("/trips/{id}/finish", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Viagem não encontrado"));
    }

    @Test
    @DisplayName("a malformed trip id is rejected")
    void shouldRejectWhenTripIdIsNotAUuid() throws Exception {
        User driver = data.driver();

        mockMvc.perform(put("/trips/{id}/finish", "not-a-uuid")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"));
    }

    @Test
    @DisplayName("request without a token is rejected")
    void shouldRejectWhenNotAuthenticated() throws Exception {
        Trip trip = givenActiveTrip(data.route(data.van(data.driver())));

        mockMvc.perform(put("/trips/{id}/finish", trip.getId()))
                .andExpect(status().isUnauthorized());

        assertThatStillActive(trip);
    }

    @Test
    @DisplayName("a driver cannot finish a trip on another driver's route")
    void shouldRejectWhenDriverDoesNotOwnTheVan() throws Exception {
        User driver = data.driver();
        User anotherDriver = data.driver();
        Trip trip = givenActiveTrip(data.route(data.van(anotherDriver)));

        mockMvc.perform(put("/trips/{id}/finish", trip.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Não foi possível finalizar viagem"))
                .andExpect(jsonPath("$.detail").value("Driver so pode finalizar viagem que pertence a ele."));

        assertThatStillActive(trip);
    }

    @Test
    @DisplayName("a parent cannot finish a trip")
    void shouldRejectWhenAuthenticatedAsParent() throws Exception {
        User parent = data.parent();
        Trip trip = givenActiveTrip(data.route(data.van(data.driver())));

        mockMvc.perform(put("/trips/{id}/finish", trip.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isForbidden());

        assertThatStillActive(trip);
    }

    @Test
    @DisplayName("an admin cannot finish a trip: only the van's own driver can")
    void shouldRejectWhenAuthenticatedAsAdmin() throws Exception {
        User admin = data.admin();
        Trip trip = givenActiveTrip(data.route(data.van(data.driver())));

        mockMvc.perform(put("/trips/{id}/finish", trip.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isForbidden());

        assertThatStillActive(trip);
    }

    @Test
    @DisplayName("an already completed trip cannot be finished again")
    void shouldRejectWhenTripIsAlreadyCompleted() throws Exception {
        User driver = data.driver();
        Trip trip = givenActiveTrip(data.route(data.van(driver)));

        mockMvc.perform(put("/trips/{id}/finish", trip.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isOk());

        OffsetDateTime firstEndedAt = tripRepository.findById(trip.getId()).orElseThrow().getEndedAt();

        mockMvc.perform(put("/trips/{id}/finish", trip.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Não foi possível finalizar viagem"))
                .andExpect(jsonPath("$.detail").value("Não e possível finalizar viagem completas."));

        // o endedAt da primeira finalização não pode ser sobrescrito
        assertThat(tripRepository.findById(trip.getId()).orElseThrow().getEndedAt())
                .isEqualTo(firstEndedAt);
    }

    private Trip givenActiveTrip(Route route) {
        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setStatus(TripStatus.ACTIVE);
        trip.setStartedAt(OffsetDateTime.now().minusHours(1));

        return tripRepository.save(trip);
    }

    private void assertThatStillActive(Trip trip) {
        Trip reloaded = tripRepository.findById(trip.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TripStatus.ACTIVE);
        assertThat(reloaded.getEndedAt()).isNull();
    }
}
