package com.vantrack.vans;

import com.vantrack.support.IntegrationTestSupport;
import com.vantrack.users.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /vans - list vans")
class ListAllVansUseCaseIT extends IntegrationTestSupport {

    @Test
    @DisplayName("admin lists every van")
    void shouldListAllVansWhenNoFilterIsGiven() throws Exception {
        User admin = data.admin();
        User driver = data.driver();
        data.van(driver, "AAA1111");
        data.van(driver, "BBB2222");

        mockMvc.perform(get("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].plate", containsInAnyOrder("AAA1111", "BBB2222")));
    }

    @Test
    @DisplayName("plate filter matches partially and ignores case")
    void shouldFilterVansByPartialPlateIgnoringCase() throws Exception {
        User admin = data.admin();
        User driver = data.driver();
        data.van(driver, "XYZ1234");
        data.van(driver, "XYZ9999");
        data.van(driver, "QWE5555");

        mockMvc.perform(get("/vans")
                        .param("plate", "xyz")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].plate", containsInAnyOrder("XYZ1234", "XYZ9999")));
    }

    @Test
    @DisplayName("userId filter returns only the vans of that driver")
    void shouldFilterVansByDriver() throws Exception {
        User admin = data.admin();
        User driver = data.driver();
        User anotherDriver = data.driver();
        data.van(driver, "OWN1111");
        data.van(anotherDriver, "OTH2222");

        mockMvc.perform(get("/vans")
                        .param("userId", driver.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].plate").value("OWN1111"));
    }

    @Test
    @DisplayName("plate and userId filters are combined")
    void shouldCombinePlateAndDriverFilters() throws Exception {
        User admin = data.admin();
        User driver = data.driver();
        User anotherDriver = data.driver();
        data.van(driver, "CMB1111");
        data.van(driver, "OUT2222");
        data.van(anotherDriver, "CMB3333");

        mockMvc.perform(get("/vans")
                        .param("plate", "cmb")
                        .param("userId", driver.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].plate").value("CMB1111"));
    }

    @Test
    @DisplayName("a filter with no match returns an empty list")
    void shouldReturnEmptyListWhenFilterMatchesNothing() throws Exception {
        User admin = data.admin();
        User driver = data.driver();
        data.van(driver, "AAA1111");

        mockMvc.perform(get("/vans")
                        .param("userId", UUID.randomUUID().toString())
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("DRIVER without filter, see only owned vans")
    void shouldListVansThatBelongDriver() throws Exception {
        User driver = data.driver();
        User anotherDriver = data.driver();

        data.van(driver, "MIN1111");
        data.van(anotherDriver, "ALH2222");

        mockMvc.perform(get("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].plate", containsInAnyOrder("MIN1111")));
    }

    @Test
    @DisplayName("Driver with userId of another driver, the parameter is ignored")
    void shouldListVansIgnoringUserIdOfAnotherDriver() throws Exception {
        User driver = data.driver();
        User anotherDriver = data.driver();

        data.van(anotherDriver, "ALH2222");

        mockMvc.perform(get("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                        .param("userId", String.valueOf(anotherDriver.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(jsonPath("$[*].plate", not(hasItem("ALH2222"))));

    }

    @Test
    @DisplayName("Driver without vans receive an empty list ")
    void shouldReturnEmptyListWhenDriveDoesNotHaveVan() throws Exception {
        User driver = data.driver();


        mockMvc.perform(get("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // Filtro de placa continua funcionando para DRIVER, sempre dentro do escopo dele

    @Test
    @DisplayName("plate filter continues work from the drive always within his scope")
    void shouldListVansWithPlateFilterDriverOnly() throws Exception {
        User driver = data.driver();
        User anotherDriver = data.driver();

        data.van(driver, "ALH1111");
        data.van(anotherDriver, "ALH2222");

        mockMvc.perform(get("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(driver))
                            .param("plate", "ALH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].plate", contains("ALH1111")));
    }

    @Test
    @DisplayName("parent has no access to the van list")
    void shouldRejectWhenAuthenticatedAsParent() throws Exception {
        User parent = data.parent();

        mockMvc.perform(get("/vans")
                        .header(HttpHeaders.AUTHORIZATION, data.bearerFor(parent)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso Negado"));
    }

    @Test
    @DisplayName("request without a token is rejected")
    void shouldRejectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/vans"))
                .andExpect(status().isUnauthorized());
    }
}
