package com.ferro.progress.interfaces.rest;

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
class ProgressFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String token;
    private String otherToken;
    private String exerciseId;

    @BeforeEach
    void setUp() throws Exception {
        token = register("progress-user");
        otherToken = register("progress-intruder");
        exerciseId = jdbc.queryForObject(
                "SELECT id::text FROM exercises WHERE name_es = 'Press de banca con barra'", String.class);
    }

    @Test
    void exposes_history_detail_prs_and_summary() throws Exception {
        String sessionId = completedSession();

        mockMvc.perform(get("/api/v1/progress/sessions").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sessionId))
                .andExpect(jsonPath("$.content[0].setCount").value(2))
                .andExpect(jsonPath("$.content[0].hasPr").value(true))
                .andExpect(jsonPath("$.content[0].status").value("FINISHED"));

        mockMvc.perform(get("/api/v1/progress/sessions")
                        .param("exerciseId", exerciseId)
                        .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/progress/sessions/" + sessionId).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/progress/sessions/" + sessionId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets.length()").value(2))
                .andExpect(jsonPath("$.exercises[0].personalRecords.length()").value(2))
                .andExpect(jsonPath("$.exercises[0].volume").value(1490.0));

        mockMvc.perform(get("/api/v1/progress/prs").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exerciseName").value("Press de banca con barra"))
                .andExpect(jsonPath("$[0].current.length()").value(2));

        mockMvc.perform(get("/api/v1/progress/summary").param("weeks", "4").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks").value(4))
                .andExpect(jsonPath("$.current.sessions").value(1))
                .andExpect(jsonPath("$.current.volume").value(1490.0))
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.muscleDistribution[0].muscleGroup").value("CHEST"));

        mockMvc.perform(get("/api/v1/progress/streak").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(1))
                .andExpect(jsonPath("$.best").value(1));

        mockMvc.perform(get("/api/v1/progress/volume").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].volume").value(1490.0));
    }

    @Test
    void recalculates_volume_and_prs_when_editing_a_finished_session() throws Exception {
        String sessionId = completedSession();
        String detail = mockMvc.perform(
                        get("/api/v1/progress/sessions/" + sessionId).header("Authorization", bearer(token)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String entryId = JsonPath.read(detail, "$.exercises[0].id");
        String firstSetId = JsonPath.read(detail, "$.exercises[0].sets[0].id");

        mockMvc.perform(patch("/api/v1/workout-sessions/" + sessionId + "/exercises/" + entryId + "/sets/" + firstSetId)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"reps\":4,\"weight\":50,\"setType\":\"NORMAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVolume").value(1050.0));

        mockMvc.perform(get("/api/v1/progress/prs").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].current[?(@.type == 'MAX_WEIGHT')].value")
                        .value(85.0));

        mockMvc.perform(delete("/api/v1/workout-sessions/" + sessionId + "/exercises/" + entryId + "/sets/"
                                + firstSetId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVolume").value(850.0));

        mockMvc.perform(delete("/api/v1/workout-sessions/" + sessionId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/progress/sessions").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    private String completedSession() throws Exception {
        String body = mockMvc.perform(post("/api/v1/workout-sessions").header("Authorization", bearer(token)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String sessionId = JsonPath.read(body, "$.id");
        String exerciseBody = mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/exercises")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + exerciseId + "\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String entryId = JsonPath.read(exerciseBody, "$.exercises[0].id");
        for (String set : new String[] {
            "{\"reps\":8,\"weight\":80,\"setType\":\"NORMAL\"}", "{\"reps\":10,\"weight\":85,\"setType\":\"NORMAL\"}"
        }) {
            mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/exercises/" + entryId + "/sets")
                            .header("Authorization", bearer(token))
                            .contentType(APPLICATION_JSON)
                            .content(set))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/v1/workout-sessions/" + sessionId + "/finish")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        return sessionId;
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
