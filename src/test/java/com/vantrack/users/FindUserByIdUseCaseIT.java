package com.vantrack.users;

import com.vantrack.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /users/{id} and GET /users/me - find user")
class FindUserByIdUseCaseIT extends IntegrationTestSupport {

    @Test
    @DisplayName("admin finds a user by id")
    void shouldReturnUserWhenAuthenticatedAsAdmin() throws Exception {
        User admin = data.admin();
        User driver = data.userNamed("Motorista Buscado", UserRole.DRIVER);

        mockMvc.perform(get("/users/{id}", driver.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(driver.getId().toString()))
                .andExpect(jsonPath("$.name").value("Motorista Buscado"))
                .andExpect(jsonPath("$.email").value(driver.getEmail()))
                .andExpect(jsonPath("$.role").value(UserRole.DRIVER.name()));
    }

    @Test
    @DisplayName("unknown id returns not found")
    void shouldRejectWhenUserDoesNotExist() throws Exception {
        User admin = data.admin();

        mockMvc.perform(get("/users/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Usuário não encontrado"));
    }

    @Test
    @DisplayName("an id that is not a uuid is rejected")
    void shouldRejectWhenIdIsNotAUuid() throws Exception {
        User admin = data.admin();

        mockMvc.perform(get("/users/{id}", "nao-e-uuid")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("driver cannot look up other users by id")
    void shouldRejectWhenAuthenticatedAsDriver() throws Exception {
        User driver = data.driver();
        User other = data.parent();

        mockMvc.perform(get("/users/{id}", other.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso Negado"));
    }

    @Test
    @DisplayName("request without a token is rejected")
    void shouldRejectWhenNotAuthenticated() throws Exception {
        User driver = data.driver();

        mockMvc.perform(get("/users/{id}", driver.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/me returns the authenticated admin")
    void shouldReturnOwnProfileWhenAuthenticatedAsAdmin() throws Exception {
        User admin = data.admin();

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId().toString()))
                .andExpect(jsonPath("$.role").value(UserRole.ADMIN.name()));
    }

    @Test
    @DisplayName("/me returns the authenticated driver")
    void shouldReturnOwnProfileWhenAuthenticatedAsDriver() throws Exception {
        User driver = data.driver();

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(driver.getId().toString()))
                .andExpect(jsonPath("$.role").value(UserRole.DRIVER.name()));
    }

    @Test
    @DisplayName("/me returns the authenticated parent")
    void shouldReturnOwnProfileWhenAuthenticatedAsParent() throws Exception {
        User parent = data.parent();

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(parent.getId().toString()))
                .andExpect(jsonPath("$.role").value(UserRole.PARENT.name()));
    }

    @Test
    @DisplayName("/me without a token is rejected")
    void shouldRejectMeWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
