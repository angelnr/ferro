package com.ferro.catalog.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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
class ExerciseStatsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void detail_computes_prs_volume_e1rm_and_session_count() throws Exception {
        String email = "stats" + UUID.randomUUID().toString().replace("-", "") + "@ferro.dev";
        String registerBody = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"displayName":"Stats","email":"%s","password":"Ferro12345","healthConsent":true}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonPath.read(registerBody, "$.accessToken");
        UUID userId = UUID.fromString(JsonPath.read(registerBody, "$.user.id"));

        String customBody = mockMvc.perform(post("/api/v1/exercises")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nameEs":"Sentadilla de stats","nameEn":"Stats Squat",
                                 "muscleGroup":"QUADS","equipment":"BARBELL","mechanics":"COMPOUND"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID exerciseId = UUID.fromString(JsonPath.read(customBody, "$.id"));

        UUID sessionId = UUID.randomUUID();
        UUID sessionExerciseId = UUID.randomUUID();
        UUID setId = UUID.randomUUID();
        jdbc.update(
                "insert into workout_sessions (id, user_id, name, status, started_at, finished_at) "
                        + "values (?, ?, 'Stats session', 'FINISHED', now() - interval '1 day', now())",
                sessionId,
                userId);
        jdbc.update(
                "insert into session_exercises (id, session_id, exercise_id, position) values (?, ?, ?, 0)",
                sessionExerciseId,
                sessionId,
                exerciseId);
        jdbc.update(
                "insert into session_sets (id, session_exercise_id, position, reps, weight) values (?, ?, 0, 10, 100)",
                setId,
                sessionExerciseId);
        jdbc.update(
                "insert into personal_records (id, user_id, exercise_id, pr_type, value, achieved_at, session_set_id) "
                        + "values (?, ?, ?, 'MAX_WEIGHT', 100, now(), ?)",
                UUID.randomUUID(),
                userId,
                exerciseId,
                setId);

        mockMvc.perform(get("/api/v1/exercises/" + exerciseId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.sessionCount").value(1))
                .andExpect(jsonPath("$.stats.totalVolume").value(1000))
                .andExpect(jsonPath("$.stats.bestE1rm").isNumber())
                .andExpect(jsonPath("$.stats.personalRecords[0].type").value("MAX_WEIGHT"))
                .andExpect(jsonPath("$.stats.e1rmSeries.length()").value(1));
    }
}
