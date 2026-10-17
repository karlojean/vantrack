package com.vantrack.routes;

import com.vantrack.routes.web.dto.AddParentRequest;
import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.tracking.UserRouteRepository;
import com.vantrack.users.User;
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

@DisplayName("POST /routes/{id}/parent - add parent to route")
class AddParentUseCaseIT extends IntegrationTestSupport {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRouteRepository userRouteRepository;

    @Test
    @DisplayName("driver adds a parent to their own route")
    void shouldAddParentWhenDriverOwnsTheRoute() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        User parent = data.parent();

        AddParentRequest request = new AddParentRequest(parent.getId(), "Maria Clara");

        mockMvc.perform(post("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(userRouteRepository.existsByRouteAndParent(route, parent)).isTrue();
        assertThat(userRouteRepository.findAll())
                .extracting(UserRoute::getStudentName)
                .containsExactly("Maria Clara");
    }

    @Test
    @DisplayName("admin adds a parent to another driver's route")
    void shouldAddParentWhenAuthenticatedAsAdmin() throws Exception {
        User admin = data.admin();
        Route route = data.route(data.van(data.driver()));
        User parent = data.parent();

        AddParentRequest request = new AddParentRequest(parent.getId(), "Pedro");

        mockMvc.perform(post("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(userRouteRepository.existsByRouteAndParent(route, parent)).isTrue();
    }

    @Test
    @DisplayName("driver cannot add a parent to another driver's route")
    void shouldRejectWhenDriverDoesNotOwnTheRoute() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(data.driver()));
        User parent = data.parent();

        AddParentRequest request = new AddParentRequest(parent.getId(), "Pedro");

        mockMvc.perform(post("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Não foi possível adicionar pai"));

        assertThat(userRouteRepository.count()).isZero();
    }

    @Test
    @DisplayName("parent cannot add a parent to a route")
    void shouldRejectWhenAuthenticatedAsParent() throws Exception {
        User parent = data.parent();
        Route route = data.route(data.van(data.driver()));

        AddParentRequest request = new AddParentRequest(parent.getId(), "Pedro");

        mockMvc.perform(post("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        assertThat(userRouteRepository.count()).isZero();
    }

    @Test
    @DisplayName("the linked user must have the PARENT role")
    void shouldRejectWhenLinkedUserIsNotParent() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        User anotherDriver = data.driver();

        AddParentRequest request = new AddParentRequest(anotherDriver.getId(), "Pedro");

        mockMvc.perform(post("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Não foi possível salvar pai"));

        assertThat(userRouteRepository.count()).isZero();
    }

    @Test
    @DisplayName("the given route must exist")
    void shouldRejectWhenRouteDoesNotExist() throws Exception {
        User admin = data.admin();
        User parent = data.parent();

        AddParentRequest request = new AddParentRequest(parent.getId(), "Pedro");

        mockMvc.perform(post("/routes/{id}/parent", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Rota não encontrado"));
    }

    @Test
    @DisplayName("the given parent must exist")
    void shouldRejectWhenParentDoesNotExist() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));

        AddParentRequest request = new AddParentRequest(UUID.randomUUID(), "Pedro");

        mockMvc.perform(post("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Usuário não encontrado"));

        assertThat(userRouteRepository.count()).isZero();
    }

    @Test
    @DisplayName("request without a token is rejected")
    void shouldRejectWhenNotAuthenticated() throws Exception {
        Route route = data.route(data.van(data.driver()));
        User parent = data.parent();

        AddParentRequest request = new AddParentRequest(parent.getId(), "Pedro");

        mockMvc.perform(post("/routes/{id}/parent", route.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("empty body is rejected with every field error")
    void shouldRejectWhenBodyIsEmpty() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));

        mockMvc.perform(post("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.parentId").exists())
                .andExpect(jsonPath("$.errors.studentName").exists());

        assertThat(userRouteRepository.count()).isZero();
    }

    @Test
    @DisplayName("blank student name is rejected")
    void shouldRejectWhenStudentNameIsBlank() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        User parent = data.parent();

        AddParentRequest request = new AddParentRequest(parent.getId(), "   ");

        mockMvc.perform(post("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studentName").exists());
    }

    @Test
    @DisplayName("student name longer than 200 characters is rejected")
    void shouldRejectWhenStudentNameIsTooLong() throws Exception {
        User driver = data.driver();
        Route route = data.route(data.van(driver));
        User parent = data.parent();

        AddParentRequest request = new AddParentRequest(parent.getId(), "A".repeat(201));

        mockMvc.perform(post("/routes/{id}/parent", route.getId())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studentName").exists());
    }
}
