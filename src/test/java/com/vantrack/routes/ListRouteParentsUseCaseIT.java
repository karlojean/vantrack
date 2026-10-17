package com.vantrack.routes;

import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.users.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /routes/{id}/parent - list route parents")
class ListRouteParentsUseCaseIT extends IntegrationTestSupport {

    @Test
    @DisplayName("driver lists the parents of their own route")
    void shouldListParentsWhenDriverOwnsTheRoute() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        User parent = data.parent();
        User anotherParent = data.parent();
        data.linkParent(parent, route);
        data.linkParent(anotherParent, route);

        mockMvc.perform(get("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].parentId", containsInAnyOrder(
                        parent.getId().toString(), anotherParent.getId().toString())))
                .andExpect(jsonPath("$[0].studentName").value("John Peter"));
    }

    @Test
    @DisplayName("admin lists the parents of another driver's route")
    void shouldListParentsWhenAuthenticatedAsAdmin() throws Exception {
        User admin = data.admin();
        Route route = data.route(data.van(data.driver()));
        User parent = data.parent();
        data.linkParent(parent, route);

        mockMvc.perform(get("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].parentId").value(parent.getId().toString()));
    }

    @Test
    @DisplayName("only the parents of the requested route are listed")
    void shouldListOnlyParentsOfTheRequestedRoute() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        Route anotherRoute = data.route(data.van(driver));
        User parent = data.parent();
        data.linkParent(parent, route);
        data.linkParent(data.parent(), anotherRoute);

        mockMvc.perform(get("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].parentId").value(parent.getId().toString()));
    }

    @Test
    @DisplayName("route without parents returns an empty list")
    void shouldReturnEmptyListWhenRouteHasNoParents() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));

        mockMvc.perform(get("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("driver cannot list the parents of another driver's route")
    void shouldRejectWhenDriverDoesNotOwnTheRoute() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(data.driver()));
        data.linkParent(data.parent(), route);

        mockMvc.perform(get("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("parent cannot list the parents of a route")
    void shouldRejectWhenAuthenticatedAsParent() throws Exception {
        User parent = data.parent();
        Route route = data.route(data.van(data.driver()));
        data.linkParent(parent, route);

        mockMvc.perform(get("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("the given route must exist")
    void shouldRejectWhenRouteDoesNotExist() throws Exception {
        User admin = data.admin();

        mockMvc.perform(get("/routes/{id}/parent", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Rota não encontrado"));
    }

    @Test
    @DisplayName("request without a token is rejected")
    void shouldRejectWhenNotAuthenticated() throws Exception {
        Route route = data.route(data.van(data.driver()));

        mockMvc.perform(get("/routes/{id}/parent", route.getId()))
                .andExpect(status().isUnauthorized());
    }
}
