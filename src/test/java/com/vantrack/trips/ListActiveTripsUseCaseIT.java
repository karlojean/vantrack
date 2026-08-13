package com.vantrack.trips;

import com.vantrack.routes.Route;
import com.vantrack.routes.UserRoute;
import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.tracking.UserRouteRepository;
import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /trips/active - list active trips")
class ListActiveTripsUseCaseIT extends IntegrationTestSupport {

    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private UserRouteRepository userRouteRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("parent sees an active trip from a linked route")
    void shouldListActiveTripFromLinkedRouteForParent() throws Exception {
        User parent = data.parent();
        Route route = data.route(data.van(data.driver()));
        link(parent, route, "Student");
        Trip trip = trip(route, TripStatus.ACTIVE);

        mockMvc.perform(get("/trips/active")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(trip.getId().toString()))
                .andExpect(jsonPath("$[0].routeId").value(route.getId().toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("parent does not see an active trip from an unlinked route")
    void shouldNotListActiveTripFromUnlinkedRouteForParent() throws Exception {
        User parent = data.parent();
        trip(data.route(data.van(data.driver())), TripStatus.ACTIVE);

        mockMvc.perform(get("/trips/active")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("multiple students on the same route do not duplicate its active trip")
    void shouldNotDuplicateTripWhenParentHasMultipleStudentsOnRoute() throws Exception {
        User parent = data.parent();
        Route route = data.route(data.van(data.driver()));
        link(parent, route, "Student A");
        link(parent, route, "Student B");
        Trip trip = trip(route, TripStatus.ACTIVE);

        mockMvc.perform(get("/trips/active")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(trip.getId().toString()));
    }

    @Test
    @DisplayName("parent does not see a completed trip from a linked route")
    void shouldNotListCompletedTripForParent() throws Exception {
        User parent = data.parent();
        Route route = data.route(data.van(data.driver()));
        link(parent, route, "Student");
        trip(route, TripStatus.COMPLETED);

        mockMvc.perform(get("/trips/active")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("admin sees every active trip and no completed trip")
    void shouldListEveryActiveTripForAdmin() throws Exception {
        User admin = data.admin();
        Trip firstActiveTrip = trip(data.route(data.van(data.driver())), TripStatus.ACTIVE);
        Trip secondActiveTrip = trip(data.route(data.van(data.driver())), TripStatus.ACTIVE);
        trip(data.route(data.van(data.driver())), TripStatus.COMPLETED);

        mockMvc.perform(get("/trips/active")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        firstActiveTrip.getId().toString(),
                        secondActiveTrip.getId().toString())))
                .andExpect(jsonPath("$[*].status", containsInAnyOrder("ACTIVE", "ACTIVE")));
    }

    @Test
    @DisplayName("driver cannot list active trips")
    void shouldRejectDriver() throws Exception {
        User driver = data.driver();
        trip(data.route(data.van(driver)), TripStatus.ACTIVE);

        mockMvc.perform(get("/trips/active")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("request without a token is rejected")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/trips/active"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token belonging to a removed user is rejected")
    void shouldRejectTokenBelongingToRemovedUser() throws Exception {
        User parent = data.parent();
        String bearer = data.bearerFor(parent);
        userRepository.delete(parent);
        userRepository.flush();

        mockMvc.perform(get("/trips/active")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized());
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

    private void link(User parent, Route route, String studentName) {
        UserRoute userRoute = new UserRoute();
        userRoute.setParent(parent);
        userRoute.setRoute(route);
        userRoute.setStudentName(studentName);
        userRouteRepository.save(userRoute);
    }
}
