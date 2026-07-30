package com.vantrack.users;

import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.users.web.dto.CreateUserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;



@DisplayName("POST /users - create user")
class CreateUserUseCaseIT extends IntegrationTestSupport {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Admin user create a new user")
    void shouldCreateUserWhenUserIsAdmin() throws Exception {
        User admin = data.admin();

        CreateUserRequest request = new CreateUserRequest("John", "john@mail.com", "secret2121", UserRole.DRIVER);

        mockMvc.perform(post("/users")
                .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.email").value("john@mail.com"))
            .andExpect(jsonPath("$.role").value(UserRole.DRIVER.name()))
            .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(userRepository.existsByEmail("john@mail.com")).isTrue();
    }

    @Test
    @DisplayName("Password is stored hashed, never in plain text")
    void shouldStorePasswordHashed() throws Exception {
        User admin = data.admin();

        CreateUserRequest request = new CreateUserRequest("John", "john@mail.com", "secret2121", UserRole.DRIVER);

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        User created = userRepository.findByEmail("john@mail.com").orElseThrow();

        assertThat(created.getPassword()).isNotEqualTo("secret2121");
        assertThat(passwordEncoder.matches("secret2121", created.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Admin can create users with any role")
    void shouldCreateUserWithAdminRole() throws Exception {
        User admin = data.admin();

        CreateUserRequest request = new CreateUserRequest("Ana", "ana@mail.com", "secret2121", UserRole.ADMIN);

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(UserRole.ADMIN.name()));
    }

    @Test
    @DisplayName("Email already registered is rejected")
    void shouldFailToCreateUserWhenEmailAlreadyExists() throws Exception {
        User admin = data.admin();
        User existing = data.driver();

        CreateUserRequest request = new CreateUserRequest("John", existing.getEmail(), "secret2121", UserRole.PARENT);

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("E-mail já cadastrado"));

        assertThat(userRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should not be possible to create user when user is not admin")
    void shouldFailToCreateUserWhenUserIsNotAdmin() throws Exception {
        User driver = data.driver();
        CreateUserRequest request = new CreateUserRequest("John", "john@mail.com", "secret2121", UserRole.DRIVER);

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso Negado"));
    }

    @Test
    @DisplayName("Parent has no access to user creation")
    void shouldFailToCreateUserWhenUserIsParent() throws Exception {
        User parent = data.parent();
        CreateUserRequest request = new CreateUserRequest("John", "john@mail.com", "secret2121", UserRole.DRIVER);

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Request without a token is rejected")
    void shouldFailToCreateUserWhenNotAuthenticated() throws Exception {
        CreateUserRequest request = new CreateUserRequest("John", "john@mail.com", "secret2121", UserRole.DRIVER);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        assertThat(userRepository.existsByEmail("john@mail.com")).isFalse();
    }

    @Test
    @DisplayName("Empty name is rejected")
    void shouldRejectWhenNameIsEmpty() throws Exception {
        User admin = data.admin();
        CreateUserRequest request = new CreateUserRequest("", "john@mail.com", "secret2121", UserRole.DRIVER);

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("Malformed email is rejected")
    void shouldRejectWhenEmailIsMalformed() throws Exception {
        User admin = data.admin();
        CreateUserRequest request = new CreateUserRequest("John", "nao-e-um-email", "secret2121", UserRole.DRIVER);

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("Password shorter than the minimum is rejected")
    void shouldRejectWhenPasswordIsTooShort() throws Exception {
        User admin = data.admin();
        CreateUserRequest request = new CreateUserRequest("John", "john@mail.com", "curta", UserRole.DRIVER);

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("Missing role is rejected")
    void shouldRejectWhenRoleIsMissing() throws Exception {
        User admin = data.admin();
        CreateUserRequest request = new CreateUserRequest("John", "john@mail.com", "secret2121", null);

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.role").exists());
    }

    @Test
    @DisplayName("Unknown role value is rejected")
    void shouldRejectWhenRoleIsUnknown() throws Exception {
        User admin = data.admin();

        String body = objectMapper.writeValueAsString(Map.of(
                "name", "John",
                "email", "john@mail.com",
                "password", "secret2121",
                "role", "SUPERVISOR"
        ));

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
