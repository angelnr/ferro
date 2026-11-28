package com.ferro.catalog.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.ferro.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "ferro.rate-limit.enabled=false")
@AutoConfigureMockMvc
class ExerciseSearchIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;

    @BeforeEach
    void registerAndGetToken() throws Exception {
        String email = "catalog" + UUID.randomUUID().toString().replace("-", "") + "@ferro.dev";
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"displayName":"Catalog","email":"%s","password":"Ferro12345","healthConsent":true}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        token = JsonPath.read(body, "$.accessToken");
    }

    @Test
    void search_is_case_and_accent_insensitive_in_both_languages() throws Exception {
        mockMvc.perform(get("/api/v1/exercises").param("q", "press banca").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", org.hamcrest.Matchers.hasItem("Press de banca con barra")));

        mockMvc.perform(get("/api/v1/exercises").param("q", "BENCH").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThan(0)));

        mockMvc.perform(get("/api/v1/exercises").param("q", "extension").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", org.hamcrest.Matchers.hasItem("Extensión en polea")));

        mockMvc.perform(get("/api/v1/exercises")
                        .param("q", "curl de femoral en maquina")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.content[*].name", org.hamcrest.Matchers.hasItem("Curl femoral tumbado en máquina")));
    }

    @Test
    void filters_and_paginates() throws Exception {
        mockMvc.perform(get("/api/v1/exercises").param("muscle", "ADDUCTORS").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[*].name", org.hamcrest.Matchers.hasItem("Aducción de cadera en máquina")));

        mockMvc.perform(get("/api/v1/exercises")
                        .param("muscle", "CHEST")
                        .param("equipment", "BARBELL")
                        .param("page", "0")
                        .param("size", "3")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath(
                        "$.content[*].muscleGroup",
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("CHEST"))));
    }

    @Test
    void seed_has_at_least_150_exercises() throws Exception {
        String body = mockMvc.perform(
                        get("/api/v1/exercises").param("size", "1").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number total = JsonPath.read(body, "$.totalElements");
        assertThat(total.longValue()).isGreaterThanOrEqualTo(150);
    }

    @Test
    void requires_authentication() throws Exception {
        mockMvc.perform(get("/api/v1/exercises")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejects_invalid_filters() throws Exception {
        mockMvc.perform(get("/api/v1/exercises").param("muscle", "NOT_A_MUSCLE").header("Authorization", bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILTER"));
    }

    private String bearer() {
        return "Bearer " + token;
    }
}
