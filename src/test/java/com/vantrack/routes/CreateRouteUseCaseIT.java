package com.vantrack.routes;

import com.vantrack.routes.web.dto.CreateRouteRequest;
import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.users.User;
import com.vantrack.vans.Van;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /routes - create route")
class CreateRouteUseCaseIT extends IntegrationTestSupport {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RouteRepository routeRepository;

    @Test
    @DisplayName("driver creates a route for their own van")
    void shouldCreateRouteWhenDriverOwnsTheVan() throws Exception {
        User driver = data.driver();
        Van van = data.van(driver);

        CreateRouteRequest request = new CreateRouteRequest("Rota da Manhã", van.getId());

        mockMvc.perform(post("/routes")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rota da Manhã"))
                .andExpect(jsonPath("$.plate").value(van.getPlate()))
                .andExpect(jsonPath("$.driver").value(driver.getName()));

        assertThat(routeRepository.findAllByVan_Driver_Id(driver.getId()))
                .extracting(Route::getName)
                .containsExactly("Rota da Manhã");
    }

    @Test
    @DisplayName("admin creates a route for another driver's van")
    void shouldCreateRouteWhenAuthenticatedAsAdmin() throws Exception {
        User admin = data.admin();
        User driver = data.driver();
        Van van = data.van(driver);

        CreateRouteRequest request = new CreateRouteRequest("Rota da Tarde", van.getId());

        mockMvc.perform(post("/routes")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rota da Tarde"))
                .andExpect(jsonPath("$.driver").value(driver.getName()));

        assertThat(routeRepository.findAllByVan_Driver_Id(driver.getId())).hasSize(1);
    }

    @Test
    @DisplayName("driver cannot create a route for another driver's van")
    void shouldRejectWhenDriverDoesNotOwnTheVan() throws Exception {
        User driver = data.driver();
        User anotherDriver = data.driver();
        Van van = data.van(anotherDriver);

        CreateRouteRequest request = new CreateRouteRequest("Rota Alheia", van.getId());

        mockMvc.perform(post("/routes")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Erro ao criar rota"));

        assertThat(routeRepository.count()).isZero();
    }

    @Test
    @DisplayName("parent cannot create a route")
    void shouldRejectWhenAuthenticatedAsParent() throws Exception {
        User parent = data.parent();
        User driver = data.driver();
        Van van = data.van(driver);

        CreateRouteRequest request = new CreateRouteRequest("Rota do Responsável", van.getId());

        mockMvc.perform(post("/routes")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("the given van must exist")
    void shouldRejectWhenVanDoesNotExist() throws Exception {
        User admin = data.admin();

        CreateRouteRequest request = new CreateRouteRequest("Rota Fantasma", UUID.randomUUID());

        mockMvc.perform(post("/routes")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Van não encontrado"));
    }

    @Test
    @DisplayName("request without a token is rejected")
    void shouldRejectWhenNotAuthenticated() throws Exception {
        User driver = data.driver();
        Van van = data.van(driver);

        CreateRouteRequest request = new CreateRouteRequest("Rota Anônima", van.getId());

        mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("empty name is rejected")
    void shouldRejectWhenNameIsEmpty() throws Exception {
        User driver = data.driver();
        Van van = data.van(driver);

        CreateRouteRequest request = new CreateRouteRequest("", van.getId());

        mockMvc.perform(post("/routes")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("name longer than 200 characters is rejected")
    void shouldRejectWhenNameIsTooLong() throws Exception {
        User driver = data.driver();
        Van van = data.van(driver);

        CreateRouteRequest request = new CreateRouteRequest("R".repeat(201), van.getId());

        mockMvc.perform(post("/routes")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("missing vanId is rejected")
    void shouldRejectWhenVanIdIsMissing() throws Exception {
        User driver = data.driver();

        CreateRouteRequest request = new CreateRouteRequest("Rota sem Van", null);

        mockMvc.perform(post("/routes")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.vanId").exists());
    }
}
