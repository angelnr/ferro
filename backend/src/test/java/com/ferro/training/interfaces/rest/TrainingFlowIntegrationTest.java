package com.ferro.training.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
