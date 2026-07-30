package com.vantrack.vans;

import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.users.User;
import com.vantrack.vans.web.dto.CreateVanRequest;
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

@DisplayName("POST /vans — create van")
class CreateVanUseCaseIT extends IntegrationTestSupport {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private VanRepository vanRepository;

    @Test
    @DisplayName("driver registers a van for themselves")
    void shouldCreateVanWhenDriverRegistersForThemselves() throws Exception {
        User driver = data.driver();

        CreateVanRequest request = new CreateVanRequest("ABC1D23", driver.getId());

        mockMvc.perform(post("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.plate").value("ABC1D23"));

        assertThat(vanRepository.existsByPlate("ABC1D23")).isTrue();
    }

    @Test
    @DisplayName("admin registers a van for a driver")
    void shouldCreateVanWhenAdminRegistersForDriver() throws Exception {
        User admin = data.admin();
        User driver = data.driver();

        CreateVanRequest request = new CreateVanRequest("XYZ9K88", driver.getId());

        mockMvc.perform(post("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plate").value("XYZ9K88"));
    }

    @Test
    @DisplayName("plate already registered is rejected")
    void shouldRejectWhenPlateAlreadyExists() throws Exception {
        User driver = data.driver();
        data.van(driver, "DUP1234");

        CreateVanRequest request = new CreateVanRequest("DUP1234", driver.getId());

        mockMvc.perform(post("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Erro ao criar van"));
    }

    @Test
    @DisplayName("driver cannot register a van for another driver")
    void shouldRejectWhenDriverRegistersForAnotherUser() throws Exception {
        User driver = data.driver();
        User anotherDriver = data.driver();

        CreateVanRequest request = new CreateVanRequest("FOR1234", anotherDriver.getId());

        mockMvc.perform(post("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Sem permissão"));
    }

    @Test
    @DisplayName("van can only be assigned to a user with the driver role")
    void shouldRejectWhenOwnerIsNotADriver() throws Exception {
        User admin = data.admin();
        User parent = data.parent();

        CreateVanRequest request = new CreateVanRequest("PAR1234", parent.getId());

        mockMvc.perform(post("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("the given driver must exist")
    void shouldRejectWhenDriverDoesNotExist() throws Exception {
        User admin = data.admin();

        CreateVanRequest request = new CreateVanRequest("NOT1234", UUID.randomUUID());

        mockMvc.perform(post("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("parent role has no access to van registration")
    void shouldRejectWhenRoleHasNoAccessToResource() throws Exception {
        User parent = data.parent();

        CreateVanRequest request = new CreateVanRequest("PRT1234", parent.getId());

        mockMvc.perform(post("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("request without a token is rejected")
    void shouldRejectWhenNotAuthenticated() throws Exception {
        CreateVanRequest request = new CreateVanRequest("ANN1234", UUID.randomUUID());

        mockMvc.perform(post("/vans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("plate with an invalid length is rejected")
    void shouldRejectWhenPlateLengthIsInvalid() throws Exception {
        User driver = data.driver();

        CreateVanRequest request = new CreateVanRequest("ABC12", driver.getId());

        mockMvc.perform(post("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
