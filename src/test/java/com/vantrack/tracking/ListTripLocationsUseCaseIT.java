package com.vantrack.tracking;

import com.vantrack.routes.Route;
import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.trips.Trip;
import com.vantrack.trips.TripRepository;
import com.vantrack.trips.TripStatus;
import com.vantrack.users.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("List trip locations")
class ListTripLocationsUseCaseIT extends IntegrationTestSupport {

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-10-15T07:00:00-03:00");

    @Autowired
    private ListTripLocationsUseCase listTripLocationsUseCase;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("returns the trip's locations from oldest to newest")
    void shouldReturnLocationsInChronologicalOrder() throws Exception {
        User parent = data.parent();
        Route route = data.route(data.van(data.driver()));
        data.linkParent(parent, route);
        Trip trip = givenActiveTrip(route);

        UUID second = givenLocation(trip, "-23.551000", "-46.634000", T0.plusSeconds(5));
        UUID third = givenLocation(trip, "-23.552000", "-46.635000", T0.plusSeconds(10));
        UUID first = givenLocation(trip, "-23.550000", "-46.633000", T0);

        mockMvc.perform(get("/trackings/" + trip.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].id").value(contains(first.toString(), second.toString(), third.toString())));
    }

    @Test
    @DisplayName("returns only the locations of the requested trip")
    void shouldReturnOnlyLocationsOfTheGivenTrip() throws Exception {
        User parent = data.parent();
        Route route = data.route(data.van(data.driver()));
        data.linkParent(parent, route);
        Trip target = givenActiveTrip(route);
        Trip other = givenActiveTrip(data.route(data.van(data.driver())));

        UUID fromTarget = givenLocation(target, "-23.550000", "-46.633000", T0);
        UUID fromOther = givenLocation(other, "-22.900000", "-43.170000", T0);

        mockMvc.perform(get("/trackings/" + target.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", equalTo(fromTarget.toString())))
                .andExpect(jsonPath("$[*].id").value(not(hasItem(fromOther.toString()))));
    }

    @Test
    @DisplayName("a trip without locations returns an empty list")
    void shouldReturnEmptyWhenTripHasNoLocations() throws Exception {
        User parent = data.parent();
        Route route = data.route(data.van(data.driver()));
        data.linkParent(parent, route);
        Trip trip = givenActiveTrip(route);

        mockMvc.perform(get("/trackings/" + trip.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("the given trip must exist")
    void shouldRejectWhenTripDoesNotExist() throws Exception {
        User parent = data.parent();
        UUID missingTripId = UUID.randomUUID();

        mockMvc.perform(get("/trackings/" + missingTripId)
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("an admin can read any trip's locations")
    void shouldAllowAdmin() throws Exception {
        User admin = data.admin();
        Trip trip = givenActiveTrip(data.route(data.van(data.driver())));
        givenLocation(trip, "-23.550000", "-46.633000", T0);

        mockMvc.perform(get("/trackings/" + trip.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("a parent not linked to the route cannot read the locations")
    void shouldRejectParentNotLinkedToRoute() throws Exception {
        User outsider = data.parent();
        Trip trip = givenActiveTrip(data.route(data.van(data.driver())));
        givenLocation(trip, "-23.550000", "-46.633000", T0);

        mockMvc.perform(get("/trackings/" + trip.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(outsider)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("the van's driver reading the locations")
    void shouldAllowTheVansOwnDriver() throws Exception {
        User driver = data.driver();
        Trip trip = givenActiveTrip(data.route(data.van(driver)));
        givenLocation(trip, "-23.550000", "-46.633000", T0);

        mockMvc.perform(get("/trackings/" + trip.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("a driver not linked to the van cannot read the location")
    void shouldRejectDriverNotLinkedToVan() throws Exception {
        User driver = data.driver();
        User anotherDriver = data.driver();
        Trip trip = givenActiveTrip(data.route(data.van(driver)));
        givenLocation(trip, "-23.550000", "-46.633000", T0);

        mockMvc.perform(get("/trackings/" + trip.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(anotherDriver)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("reading the locations of a completed trip")
    void shouldAllowCompletedTrip() throws Exception {
        User parent = data.parent();
        Route route = data.route(data.van(data.driver()));
        data.linkParent(parent, route);
        Trip trip = givenCompletedTrip(route);
        givenLocation(trip, "-23.550000", "-46.633000", T0);

        mockMvc.perform(get("/trackings/" + trip.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private Trip givenActiveTrip(Route route) {
        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setStatus(TripStatus.ACTIVE);
        trip.setStartedAt(T0.minusMinutes(1));

        return tripRepository.save(trip);
    }

    private Trip givenCompletedTrip(Route route) {
        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setStatus(TripStatus.COMPLETED);
        trip.setStartedAt(T0.minusMinutes(1));
        trip.setEndedAt(T0.plusHours(1));

        return tripRepository.save(trip);
    }

    private UUID givenLocation(Trip trip, String latitude, String longitude, OffsetDateTime createdAt) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO trip_locations (id, trip_id, latitude, longitude, created_at) VALUES (?, ?, ?, ?, ?)",
                id, trip.getId(), new BigDecimal(latitude), new BigDecimal(longitude), createdAt);

        return id;
    }
}
