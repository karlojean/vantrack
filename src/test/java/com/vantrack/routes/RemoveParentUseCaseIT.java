package com.vantrack.routes;

import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.users.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DELETE /routes/{id}/parent/{parentId} - remove parent to route")
public class RemoveParentUseCaseIT extends IntegrationTestSupport {

    @Autowired
    private UserRouteRepository userRouteRepository;
    @Autowired
    private RouteRepository routeRepository;

    @Test
    @DisplayName("driver remove a parent to their own route")
    void shouldRemoveParentWhenDriverOwnsTheRoute() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        User parent = data.parent();

        data.linkParent(parent, route);

        mockMvc.perform(delete("/routes/{id}/parent/{parentId}", route.getId(), parent.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(userRouteRepository.existsByRouteAndParent(route, parent)).isFalse();
    }

    @Test
    @DisplayName("admin removes a parent from any route")
    void shouldRemoveParentWhenIsAdminUser() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        User parent = data.parent();
        User admin = data.admin();

        data.linkParent(parent, route);

        mockMvc.perform(delete("/routes/{id}/parent/{parentId}", route.getId(), parent.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(userRouteRepository.existsByRouteAndParent(route, parent)).isFalse();
    }

    @Test
    @DisplayName("driver cannot remove a parent from another driver's route")
    void shouldRejectWhenDriverNotOwnRoute() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        User parent = data.parent();
        User anotherDriver = data.driver();

        data.linkParent(parent, route);

        mockMvc.perform(delete("/routes/{id}/parent/{parentId}", route.getId(), parent.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(anotherDriver))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        assertThat(userRouteRepository.existsByRouteAndParent(route, parent)).isTrue();
    }

    @Test
    @DisplayName("parent cannot remove a parent from a route")
    void shouldRejectWhenIsParentUser() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        User parent = data.parent();
        User otherParent = data.parent();

        data.linkParent(parent, route);

        mockMvc.perform(delete("/routes/{id}/parent/{parentId}", route.getId(), parent.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(otherParent))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        assertThat(userRouteRepository.existsByRouteAndParent(route, parent)).isTrue();
    }

    @Test
    @DisplayName("return 404 when route does not exist")
    void shouldRejectWhenRouteNotExist() throws Exception {
        User driver = data.driver();
        User parent = data.parent();
        UUID routeId = UUID.randomUUID();

        mockMvc.perform(delete("/routes/{id}/parent/{parentId}", routeId, parent.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("return 404 when parent does not exist")
    void shouldRejectWhenParentNotExist() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        UUID parentId = UUID.randomUUID();

        mockMvc.perform(delete("/routes/{id}/parent/{parentId}", route.getId(), parentId)
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("keeps the route after removing a parent")
    void shouldKeepTheRouteWhenRemoveAParent() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        User parent = data.parent();

        data.linkParent(parent, route);

        mockMvc.perform(delete("/routes/{id}/parent/{parentId}", route.getId(), parent.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(routeRepository.existsById(route.getId())).isTrue();
    }

    @Test
    @DisplayName("keeps the parent's link to other routes")
    void shouldKeepTheParentLinkToOtherRoutes() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        Route otherRoute = data.route(data.van(driver));
        User parent = data.parent();

        data.linkParent(parent, route);
        data.linkParent(parent, otherRoute);

        mockMvc.perform(delete("/routes/{id}/parent/{parentId}", route.getId(), parent.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(userRouteRepository.existsByRouteAndParent(route, parent)).isFalse();
        assertThat(userRouteRepository.existsByRouteAndParent(otherRoute, parent)).isTrue();
    }

    @Test
    @DisplayName("keeps other parents linked to the same route")
    void shouldKeepOtherParentsLinkedToRouteWhenRemoveAParent() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        User parent = data.parent();
        User otherParent = data.parent();

        data.linkParent(parent, route);
        data.linkParent(otherParent, route);

        mockMvc.perform(delete("/routes/{id}/parent/{parentId}", route.getId(), parent.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(userRouteRepository.existsByRouteAndParent(route, otherParent)).isTrue();
        assertThat(userRouteRepository.existsByRouteAndParent(route, parent)).isFalse();
        assertThat(userRouteRepository.countByRoute(route)).isEqualTo(1);
    }

    @Test
    @DisplayName("returns 401 when request has no token")
    void shouldRejectWhenRequestHasNoToken() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        User parent = data.parent();

        data.linkParent(parent, route);

        mockMvc.perform(delete("/routes/{id}/parent/{parentId}", route.getId(), parent.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        assertThat(userRouteRepository.existsByRouteAndParent(route, parent)).isTrue();
    }
}
