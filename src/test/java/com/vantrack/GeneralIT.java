package com.vantrack;

import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.users.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GeneralIT extends IntegrationTestSupport {

    @Test
    @DisplayName("Should return not found on route that does not exist")
    void shouldReturnNotFoundOnRouteThatDoesNotExist() throws Exception {
        User admin = data.admin();

        mockMvc.perform(get("/nonexistent-route")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isNotFound());
    }
}
