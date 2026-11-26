package com.ferro.training.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.ferro.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "ferro.rate-limit.enabled=false")
@AutoConfigureMockMvc
class RoutineFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String token;
    private String otherToken;
    private String pressId;
    private String squatId;

    @BeforeEach
    void setUp() throws Exception {
        token = register("routine-owner");
        otherToken = register("routine-intruder");
        pressId = exercise("Press de banca con barra");
        squatId = exercise("Sentadilla trasera con barra");
    }

    @Test
    void creates_lists_updates_duplicates_and_deletes_routines() throws Exception {
        String created = mockMvc.perform(post("/api/v1/routines")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(routineJson("PPL", 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("PPL"))
                .andExpect(jsonPath("$.exercises.length()").value(2))
                .andExpect(jsonPath("$.exercises[0].restSeconds").value(120))
                .andExpect(jsonPath("$.exercises[0].supersetGroup").value(1))
                .andExpect(jsonPath("$.exercises[0].sets.length()").value(1))
                .andExpect(jsonPath("$.exercises[0].sets[0].targetReps").value(8))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String routineId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/v1/routines").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(routineId))
                .andExpect(jsonPath("$.content[0].exerciseCount").value(2))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/routines/" + routineId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/routines/" + routineId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"PPL editada","notes":"3 días","exercises":[
                                  {"exerciseId":"%s","restSeconds":90,"sets":[
                                    {"setType":"WARMUP","targetReps":5,"targetWeight":40,"targetRir":4}
                                  ]}
                                ]}
                                """.formatted(pressId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("PPL editada"))
                .andExpect(jsonPath("$.exercises.length()").value(1))
                .andExpect(jsonPath("$.exercises[0].sets[0].setType").value("WARMUP"));

        String duplicated = mockMvc.perform(
                        post("/api/v1/routines/" + routineId + "/duplicate").header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("PPL editada (copia)"))
                .andExpect(jsonPath("$.exercises.length()").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String copyId = JsonPath.read(duplicated, "$.id");

        mockMvc.perform(post("/api/v1/routines")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(routineJson("ppl EDITADA", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROUTINE_NAME_TAKEN"));

        mockMvc.perform(delete("/api/v1/routines/" + routineId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/routines/" + routineId).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/routines/" + copyId).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void rejects_invalid_routines() throws Exception {
        mockMvc.perform(post("/api/v1/routines")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Vacía\",\"exercises\":[]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/routines")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Reps locas","exercises":[
                                  {"exerciseId":"%s","sets":[{"targetReps":0}]}
                                ]}
                                """.formatted(pressId)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/v1/routines")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Superset roto","exercises":[
                                  {"exerciseId":"%s","supersetGroup":1,"sets":[{"targetReps":8}]},
                                  {"exerciseId":"%s","sets":[{"targetReps":8}]}
                                ]}
                                """.formatted(pressId, squatId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void starts_a_session_with_pending_target_sets() throws Exception {
        String created = mockMvc.perform(post("/api/v1/routines")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(routineJson("Full body", 1)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String routineId = JsonPath.read(created, "$.id");

        String session = mockMvc.perform(
                        post("/api/v1/routines/" + routineId + "/start").header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Full body"))
                .andExpect(jsonPath("$.exercises[0].name").value("Press de banca con barra"))
                .andExpect(jsonPath("$.exercises[0].restSeconds").value(120))
                .andExpect(jsonPath("$.exercises[0].targetSets.length()").value(1))
                .andExpect(jsonPath("$.exercises[0].targetSets[0].targetReps").value(8))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String sessionId = JsonPath.read(session, "$.id");
        String entryId = JsonPath.read(session, "$.exercises[0].id");
        String targetSetId = JsonPath.read(session, "$.exercises[0].targetSets[0].id");

        mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/exercises/" + entryId + "/sets")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"reps":8,"weight":80,"setType":"NORMAL","targetSetId":"%s"}
                                """.formatted(targetSetId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reps").value(8));

        mockMvc.perform(get("/api/v1/workout-sessions/" + sessionId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets.length()").value(1))
                .andExpect(jsonPath("$.exercises[0].targetSets.length()").value(0));

        mockMvc.perform(post("/api/v1/routines/" + routineId + "/start").header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_ALREADY_ACTIVE"));

        mockMvc.perform(get("/api/v1/routines/" + routineId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets.length()").value(1));

        mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/finish")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/workout-sessions")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"routineId\":\"" + routineId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises[0].targetSets.length()").value(1));
    }

    private String routineJson(String name, int exercises) {
        String first = """
                {"exerciseId":"%s","restSeconds":120,"supersetGroup":%s,"notes":"Técnica","sets":[
                  {"setType":"NORMAL","targetReps":8,"targetWeight":60,"targetRir":2}
                ]}
                """.formatted(pressId, exercises > 1 ? "1" : "null");
        if (exercises == 1) {
            return "{\"name\":\"%s\",\"notes\":\"nota\",\"exercises\":[%s]}".formatted(name, first);
        }
        String second = """
                {"exerciseId":"%s","supersetGroup":1,"sets":[{"targetReps":10}]}
                """.formatted(squatId);
        return "{\"name\":\"%s\",\"notes\":\"nota\",\"exercises\":[%s,%s]}".formatted(name, first, second);
    }

    private String exercise(String name) {
        return jdbc.queryForObject("SELECT id::text FROM exercises WHERE name_es = ?", String.class, name);
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
