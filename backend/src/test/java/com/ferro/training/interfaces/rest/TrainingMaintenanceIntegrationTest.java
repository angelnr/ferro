package com.ferro.training.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.ferro.platform.api.EmailNotifier;
import com.ferro.support.AbstractPostgresIntegrationTest;
import com.ferro.training.application.TrainingMaintenanceService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "ferro.rate-limit.enabled=false")
@AutoConfigureMockMvc
@Import(TrainingMaintenanceIntegrationTest.NotifierConfig.class)
class TrainingMaintenanceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TrainingMaintenanceService maintenance;

    @Autowired
    private RecordingNotifier emailNotifier;

    @Test
    void auto_closes_sessions_older_than_24_hours() throws Exception {
        String email = "stale" + UUID.randomUUID().toString().replace("-", "") + "@ferro.dev";
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"displayName":"Stale","email":"%s","password":"Ferro12345","healthConsent":true}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID userId = UUID.fromString(JsonPath.read(body, "$.user.id"));
        UUID exerciseId =
                jdbc.queryForObject("SELECT id FROM exercises WHERE name_es = 'Press de banca con barra'", UUID.class);
        UUID sessionId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();

        jdbc.update(
                "insert into workout_sessions (id, user_id, name, status, started_at) "
                        + "values (?, ?, 'Old session', 'ACTIVE', now() - interval '25 hours')",
                sessionId,
                userId);
        jdbc.update(
                "insert into session_exercises (id, session_id, exercise_id, position) values (?, ?, ?, 0)",
                entryId,
                sessionId,
                exerciseId);
        jdbc.update(
                "insert into session_sets (id, session_exercise_id, position, reps, weight, logged_at) "
                        + "values (?, ?, 0, 8, 80, now() - interval '24 hours')",
                UUID.randomUUID(),
                entryId);

        assertThat(maintenance.autoCloseStaleSessions()).isEqualTo(1);

        String status =
                jdbc.queryForObject("SELECT status FROM workout_sessions WHERE id = ?", String.class, sessionId);
        Integer duration = jdbc.queryForObject(
                "SELECT duration_seconds FROM workout_sessions WHERE id = ?", Integer.class, sessionId);
        assertThat(status).isEqualTo("AUTO_CLOSED");
        assertThat(duration).isBetween(3500, 3700);
        assertThat(emailNotifier.sent()).contains(email);
        assertThat(maintenance.autoCloseStaleSessions()).isZero();
    }

    @TestConfiguration
    static class NotifierConfig {
        @Bean
        @Primary
        RecordingNotifier recordingNotifier() {
            return new RecordingNotifier();
        }
    }

    static class RecordingNotifier implements EmailNotifier {
        private final java.util.List<String> sent = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public void sendSessionAutoClosed(String recipientEmail, String locale) {
            sent.add(recipientEmail);
        }

        java.util.List<String> sent() {
            return java.util.List.copyOf(sent);
        }
    }
}
