package com.vantrack.routes;

import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.users.User;
import com.vantrack.vans.Van;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /routes - list routes")
class ListAllRoutesUseCaseIT extends IntegrationTestSupport {

    @Test
    @DisplayName("admin sees the routes of every van")
    void shouldListEveryRouteWhenAuthenticatedAsAdmin() throws Exception {
        User admin = data.admin();
        User driver = data.driver();
        User anotherDriver = data.driver();
        data.route(data.van(driver, "AAA1111"), "Rota A");
        data.route(data.van(anotherDriver, "BBB2222"), "Rota B");

        mockMvc.perform(get("/routes")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Rota A", "Rota B")));
    }

    @Test
    @DisplayName("driver sees only the routes of their own vans")
    void shouldListOnlyOwnRoutesWhenAuthenticatedAsDriver() throws Exception {
        User driver = data.driver();
        User anotherDriver = data.driver();
        Van van = data.van(driver, "MIN1111");
        data.route(van, "Minha Rota");
        data.route(data.van(anotherDriver, "ALH2222"), "Rota Alheia");

        mockMvc.perform(get("/routes")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Minha Rota"))
                .andExpect(jsonPath("$[0].plate").value("MIN1111"))
                .andExpect(jsonPath("$[0].driver").value(driver.getName()));
    }

    @Test
    @DisplayName("driver without routes gets an empty list")
    void shouldReturnEmptyListWhenDriverHasNoRoutes() throws Exception {
        User driver = data.driver();
        User anotherDriver = data.driver();
        data.route(data.van(anotherDriver, "ALH2222"), "Rota Alheia");

        mockMvc.perform(get("/routes")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("parent gets an empty list since they own no van")
    void shouldReturnEmptyListWhenAuthenticatedAsParent() throws Exception {
        User parent = data.parent();
        User driver = data.driver();
        data.route(data.van(driver, "AAA1111"), "Rota A");

        mockMvc.perform(get("/routes")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("request without a token is rejected")
    void shouldRejectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/routes"))
                .andExpect(status().isUnauthorized());
    }
}
