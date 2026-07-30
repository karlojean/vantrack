package com.vantrack.auth;

import com.vantrack.auth.web.dto.LoginRequest;
import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.support.TestDataFactory;
import com.vantrack.users.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /auth/login - authenticate user")
class LoginUseCaseIT extends IntegrationTestSupport {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("valid credentials return a usable token")
    void shouldReturnTokenWhenCredentialsAreValid() throws Exception {
        User driver = data.driver();

        LoginRequest request = new LoginRequest(driver.getEmail(), TestDataFactory.DEFAULT_PASSWORD);

        String token = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(token).isNotBlank();

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(driver.getId().toString()))
                .andExpect(jsonPath("$.email").value(driver.getEmail()));
    }

    @Test
    @DisplayName("wrong password is rejected")
    void shouldRejectWhenPasswordIsWrong() throws Exception {
        User driver = data.driver();

        LoginRequest request = new LoginRequest(driver.getEmail(), "senhaerrada123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Falha na autenticação"));
    }

    @Test
    @DisplayName("unknown email is rejected")
    void shouldRejectWhenEmailDoesNotExist() throws Exception {
        LoginRequest request = new LoginRequest("naoexiste@vantrack.test", TestDataFactory.DEFAULT_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Falha na autenticação"));
    }

    @Test
    @DisplayName("malformed email is rejected before hitting the database")
    void shouldRejectWhenEmailIsMalformed() throws Exception {
        LoginRequest request = new LoginRequest("nao-e-um-email", TestDataFactory.DEFAULT_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("password shorter than the minimum is rejected")
    void shouldRejectWhenPasswordIsTooShort() throws Exception {
        User driver = data.driver();

        LoginRequest request = new LoginRequest(driver.getEmail(), "curta");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }
}
