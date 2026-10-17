package com.vantrack.routes;

import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.users.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /me - list parent routes")
public class ListParentRoutesUseCaseIT extends IntegrationTestSupport  {

    @Test
    @DisplayName("parents lists their routes")
    void shouldListRoutesLinkedToParent() throws Exception  {
        User parent = data.parent();
        User driver = data.driver();
        Route route1 = data.route(data.van(driver, "AAA1111"), "Rota A");
        data.route(data.van(driver, "BBB2222"), "Rota B");
        data.linkParent(parent, route1);

        mockMvc.perform(get("/routes/me")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("validate route response fields")
    void shouldReturnRouteFields() throws Exception {
        User parent = data.parent();
        User driver = data.driver();
        Route route = data.route(data.van(driver, "AAA1111"), "Rota A");

        data.linkParent(parent, route);

        mockMvc.perform(get("/routes/me")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", equalTo(route.getId().toString())))
                .andExpect(jsonPath("$[0].name", equalTo("Rota A")))
                .andExpect(jsonPath("$[0].plate", equalTo("AAA1111")))
                .andExpect(jsonPath("$[0].driver", equalTo(driver.getName())));
    }

    @Test
    @DisplayName("return empty list when parent not linked to any router")
    void shouldReturnEmptyListWhenParentHasNoRoutes() throws Exception {
        User parent = data.parent();

        mockMvc.perform(get("/routes/me")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Not possible to see a route linked to another parent")
    void shouldNotListRoutesOfAnotherParent() throws Exception {
        User parent = data.parent();
        User anotherParent = data.parent();

        User driver = data.driver();
        Route route = data.route(data.van(driver, "AAA1111"), "Rota A");
        data.linkParent(anotherParent, route);

        mockMvc.perform(get("/routes/me")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(jsonPath("$[*].id", not(containsInAnyOrder(route.getId().toString()))));
    }

    @Test
    @DisplayName("parents linked to the same route both see it")
    void shouldListSharedRouteForEachParent() throws Exception {
        User parent = data.parent();
        User anotherParent = data.parent();

        User driver = data.driver();
        Route route = data.route(data.van(driver, "AAA1111"), "Rota A");
        data.linkParent(parent, route);
        data.linkParent(anotherParent, route);

        mockMvc.perform(get("/routes/me")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", equalTo(route.getId().toString())));
        ;

        mockMvc.perform(get("/routes/me")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(anotherParent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", equalTo(route.getId().toString())));
    }

    @Test
    @DisplayName("a route is listed once even when the parent has two children on it")
    void shouldNotDuplicateRouteWhenParentHasTwoChildren() throws Exception {
        User parent = data.parent();
        User driver = data.driver();
        Route route = data.route(data.van(driver, "AAA1111"), "Rota A");
        data.linkParent(parent, route, "John");
        data.linkParent(parent, route, "Mary");

        mockMvc.perform(get("/routes/me")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("driver cannot list parent routes")
    void shouldRejectWhenAuthenticatedAsDriver() throws Exception {
        User driver = data.driver();

        mockMvc.perform(get("/routes/me")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin cannot list parent routes")
    void shouldRejectWhenAuthenticatedAsAdmin() throws Exception {
        User admin = data.admin();

        mockMvc.perform(get("/routes/me")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("request without a token is rejected")
    void shouldRejectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/routes/me"))
                .andExpect(status().isUnauthorized());
    }
}
