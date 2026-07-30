package com.vantrack.users;

import com.vantrack.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /users - list users")
class ListAllUsersUseCaseIT extends IntegrationTestSupport {

    @Test
    @DisplayName("admin lists every user")
    void shouldListAllUsersWhenNoFilterIsGiven() throws Exception {
        User admin = data.admin();
        data.driver();
        data.parent();

        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("name filter matches partially and ignores case")
    void shouldFilterUsersByPartialNameIgnoringCase() throws Exception {
        User admin = data.admin();
        data.userNamed("Maria Silva", UserRole.DRIVER);
        data.userNamed("Mariana Souza", UserRole.PARENT);
        data.userNamed("João Pereira", UserRole.DRIVER);

        mockMvc.perform(get("/users")
                        .param("name", "mari")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Maria Silva", "Mariana Souza")));
    }

    @Test
    @DisplayName("role filter returns only users with that role")
    void shouldFilterUsersByRole() throws Exception {
        User admin = data.admin();
        data.driver();
        data.driver();
        data.parent();

        mockMvc.perform(get("/users")
                        .param("role", UserRole.DRIVER.name())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].role").value(UserRole.DRIVER.name()))
                .andExpect(jsonPath("$[1].role").value(UserRole.DRIVER.name()));
    }

    @Test
    @DisplayName("name and role filters are combined")
    void shouldCombineNameAndRoleFilters() throws Exception {
        User admin = data.admin();
        data.userNamed("Carlos Motorista", UserRole.DRIVER);
        data.userNamed("Carlos Responsavel", UserRole.PARENT);

        mockMvc.perform(get("/users")
                        .param("name", "carlos")
                        .param("role", UserRole.PARENT.name())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Carlos Responsavel"));
    }

    @Test
    @DisplayName("a filter with no match returns an empty list")
    void shouldReturnEmptyListWhenFilterMatchesNobody() throws Exception {
        User admin = data.admin();
        data.driver();

        mockMvc.perform(get("/users")
                        .param("name", "ninguem-com-esse-nome")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("driver has no access to the user list")
    void shouldRejectWhenAuthenticatedAsDriver() throws Exception {
        User driver = data.driver();

        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso Negado"));
    }

    @Test
    @DisplayName("parent has no access to the user list")
    void shouldRejectWhenAuthenticatedAsParent() throws Exception {
        User parent = data.parent();

        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("request without a token is rejected")
    void shouldRejectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }
}
