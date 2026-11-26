package com.ferro.training.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.ferro.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "ferro.rate-limit.enabled=false")
@AutoConfigureMockMvc
class TrainingFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String token;
    private String otherToken;
    private String exerciseId;

    @BeforeEach
    void setUp() throws Exception {
        token = register("trainer");
        otherToken = register("intruder");
        exerciseId = jdbc.queryForObject(
                "SELECT id::text FROM exercises WHERE name_es = 'Press de banca con barra'", String.class);
    }

    @Test
    void starts_logs_finishes_and_computes_prs() throws Exception {
        String sessionBody = mockMvc.perform(post("/api/v1/workout-sessions").header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String sessionId = JsonPath.read(sessionBody, "$.id");

        mockMvc.perform(post("/api/v1/workout-sessions").header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_ALREADY_ACTIVE"))
                .andExpect(jsonPath("$.activeSessionId").value(sessionId));

        mockMvc.perform(get("/api/v1/workout-sessions/active").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId));

        String exerciseBody = mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/exercises")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + exerciseId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String entryId = JsonPath.read(exerciseBody, "$.exercises[0].id");

        mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/exercises/" + entryId + "/sets")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reps\":8,\"weight\":80,\"setType\":\"NORMAL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.volume").value(640.0))
                .andExpect(jsonPath("$.e1rm").isNumber());
        mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/exercises/" + entryId + "/sets")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reps\":10,\"weight\":85,\"setType\":\"NORMAL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.volume").value(850.0));

        mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/finish")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.status").value("FINISHED"))
                .andExpect(jsonPath("$.session.totalVolume").value(1490.0))
                .andExpect(jsonPath("$.session.durationSeconds").isNumber())
                .andExpect(jsonPath("$.newPersonalRecords[*].prType", org.hamcrest.Matchers.hasItem("MAX_WEIGHT")))
                .andExpect(jsonPath("$.newPersonalRecords[*].prType", org.hamcrest.Matchers.hasItem("MAX_E1RM")));

        mockMvc.perform(get("/api/v1/workout-sessions/active").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void discards_a_session_and_hides_it_from_other_users() throws Exception {
        String body = mockMvc.perform(post("/api/v1/workout-sessions").header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String sessionId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/v1/workout-sessions/" + sessionId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/workout-sessions/" + sessionId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/workout-sessions/active").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void edits_and_deletes_active_sets_and_exercises() throws Exception {
        String sessionId = JsonPath.read(
                mockMvc.perform(post("/api/v1/workout-sessions").header("Authorization", bearer(token)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");
        String entryId = JsonPath.read(
                mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/exercises")
                                .header("Authorization", bearer(token))
                                .contentType(APPLICATION_JSON)
                                .content("{\"exerciseId\":\"" + exerciseId + "\"}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.exercises[0].id");
        String setId = JsonPath.read(
                mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/exercises/" + entryId + "/sets")
                                .header("Authorization", bearer(token))
                                .contentType(APPLICATION_JSON)
                                .content("{\"reps\":8,\"weight\":80}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");

        mockMvc.perform(patch("/api/v1/workout-sessions/" + sessionId + "/exercises/" + entryId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"restSeconds\":120,\"notes\":\"Subir carga\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].restSeconds").value(120));

        mockMvc.perform(patch("/api/v1/workout-sessions/" + sessionId + "/exercises/" + entryId + "/sets/" + setId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reps\":10,\"weight\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets[0].volume").value(900.0));

        mockMvc.perform(delete("/api/v1/workout-sessions/" + sessionId + "/exercises/" + entryId + "/sets/" + setId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets.length()").value(0));

        mockMvc.perform(delete("/api/v1/workout-sessions/" + sessionId + "/exercises/" + entryId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises.length()").value(0));
    }

    @Test
    void groups_only_adjacent_exercises_into_a_superset() throws Exception {
        String secondExercise = jdbc.queryForObject(
                "SELECT id::text FROM exercises WHERE name_es = 'Sentadilla trasera con barra'", String.class);
        String sessionId = JsonPath.read(
                mockMvc.perform(post("/api/v1/workout-sessions").header("Authorization", bearer(token)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");
        String first = JsonPath.read(
                mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/exercises")
                                .header("Authorization", bearer(token))
                                .contentType(APPLICATION_JSON)
                                .content("{\"exerciseId\":\"" + exerciseId + "\"}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.exercises[0].id");
        String second = JsonPath.read(
                mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/exercises")
                                .header("Authorization", bearer(token))
                                .contentType(APPLICATION_JSON)
                                .content("{\"exerciseId\":\"" + secondExercise + "\"}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.exercises[1].id");
        jdbc.update(
                "UPDATE session_exercises SET position = position + 1000 WHERE session_id = ?",
                UUID.fromString(sessionId));
        jdbc.update("UPDATE session_exercises SET position = 0 WHERE id = ?", UUID.fromString(first));
        jdbc.update("UPDATE session_exercises SET position = 1 WHERE id = ?", UUID.fromString(second));

        mockMvc.perform(patch("/api/v1/workout-sessions/" + sessionId + "/supersets")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"entryIds\":[\"" + first + "\",\"" + second + "\"],\"group\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].supersetGroup").value(1))
                .andExpect(jsonPath("$.exercises[1].supersetGroup").value(1));

        mockMvc.perform(patch("/api/v1/workout-sessions/" + sessionId + "/supersets")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"entryIds\":[\"" + first + "\"],\"group\":1}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void history_returns_previous_finished_sets() throws Exception {
        String sessionId = JsonPath.read(
                mockMvc.perform(post("/api/v1/workout-sessions").header("Authorization", bearer(token)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.id");
        String entryId = JsonPath.read(
                mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/exercises")
                                .header("Authorization", bearer(token))
                                .contentType(APPLICATION_JSON)
                                .content("{\"exerciseId\":\"" + exerciseId + "\"}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.exercises[0].id");
        mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/exercises/" + entryId + "/sets")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reps\":8,\"weight\":80}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/finish")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/exercises/" + exerciseId + "/history")
                        .param("limit", "3")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value(sessionId))
                .andExpect(jsonPath("$[0].sets[0].reps").value(8));
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
