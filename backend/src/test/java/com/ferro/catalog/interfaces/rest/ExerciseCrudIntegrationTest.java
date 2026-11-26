package com.ferro.catalog.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

@SpringBootTest(properties = {"ferro.rate-limit.enabled=false", "ferro.catalog.custom-limit=2"})
@AutoConfigureMockMvc
class ExerciseCrudIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String ownerToken;
    private String otherToken;

    @BeforeEach
    void createUsers() throws Exception {
        ownerToken = register("owner");
        otherToken = register("other");
    }

    @Test
    void creates_searches_updates_and_archives_a_custom_exercise() throws Exception {
        String createBody = mockMvc.perform(post("/api/v1/exercises")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nameEs":"Curl martillo en banco","nameEn":"Bench Hammer Curl",
                                 "muscleGroup":"BICEPS","secondaryMuscles":["FOREARMS"],
                                 "equipment":"DUMBBELL","mechanics":"ISOLATION","bodyweight":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = JsonPath.read(createBody, "$.id");

        mockMvc.perform(get("/api/v1/exercises")
                        .param("q", "martillo en banco")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.content[0].mine").value(true));

        mockMvc.perform(get("/api/v1/exercises/" + id).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercise.name").value("Curl martillo en banco"))
                .andExpect(jsonPath("$.stats.sessionCount").value(0));

        mockMvc.perform(patch("/api/v1/exercises/" + id)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"nameEs\":\"Curl martillo modificado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Curl martillo modificado"));

        mockMvc.perform(patch("/api/v1/exercises/" + id)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"archived\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/exercises")
                        .param("q", "martillo modificado")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void enforces_limit_and_ownership() throws Exception {
        String id = createCustom(ownerToken, "Custom 0");
        createCustom(ownerToken, "Custom 1");
        mockMvc.perform(post("/api/v1/exercises")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(APPLICATION_JSON)
                        .content(customBody("Custom 3")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LIMIT_REACHED"));

        mockMvc.perform(get("/api/v1/exercises/" + id).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/exercises/" + id)
                        .header("Authorization", bearer(otherToken))
                        .contentType(APPLICATION_JSON)
                        .content("{\"nameEs\":\"Hackeado\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejects_an_exercise_without_a_name() throws Exception {
        mockMvc.perform(post("/api/v1/exercises")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"muscleGroup":"CHEST","equipment":"BARBELL","mechanics":"COMPOUND"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private String createCustom(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/exercises")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(customBody(name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String customBody(String name) {
        return """
                {"nameEs":"%s","nameEn":"%s","muscleGroup":"CHEST",
                 "equipment":"BARBELL","mechanics":"COMPOUND"}
                """.formatted(name, name);
    }

    private String register(String prefix) throws Exception {
        String email = prefix + UUID.randomUUID().toString().replace("-", "") + "@ferro.dev";
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"displayName":"%s","email":"%s","password":"Ferro12345","healthConsent":true}
                                """.formatted(prefix, email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
