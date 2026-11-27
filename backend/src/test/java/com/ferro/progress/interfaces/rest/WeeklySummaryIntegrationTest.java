package com.ferro.progress.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.ferro.platform.api.EmailNotifier;
import com.ferro.progress.application.WeeklySummaryService;
import com.ferro.support.AbstractPostgresIntegrationTest;

@SpringBootTest(properties = "ferro.rate-limit.enabled=false")
@AutoConfigureMockMvc
class WeeklySummaryIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Instant MONDAY_0805_MADRID = Instant.parse("2026-11-30T07:05:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private WeeklySummaryService weekly;

    @Autowired
    private CapturingNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier.clear();
        jdbc.update("UPDATE feature_flags SET enabled = true WHERE key = 'email.weekly-summary'");
    }

    @Test
    void sends_weekly_summary_only_to_opted_in_users_at_monday_8() throws Exception {
        String email = "weekly" + UUID.randomUUID().toString().replace("-", "") + "@ferro.dev";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"displayName":"Weekly","email":"%s","password":"Ferro12345","healthConsent":true}
                                """.formatted(email)))
                .andExpect(status().isCreated());
        jdbc.update("UPDATE users SET email_weekly_summary = true, status = 'ACTIVE' WHERE email = ?", email);

        assertThat(weekly.sendDue(MONDAY_0805_MADRID)).isEqualTo(1);
        assertThat(notifier.recipients()).contains(email);
        assertThat(weekly.sendDue(MONDAY_0805_MADRID.plusSeconds(3600))).isZero();
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(MONDAY_0805_MADRID, ZoneOffset.UTC);
        }

        @Bean
        @Primary
        CapturingNotifier capturingNotifier() {
            return new CapturingNotifier();
        }
    }

    static class CapturingNotifier implements EmailNotifier {
        private final List<String> recipients = new CopyOnWriteArrayList<>();

        @Override
        public void sendSessionAutoClosed(String recipientEmail, String locale) {
            recipients.add(recipientEmail);
        }

        @Override
        public void sendWeeklySummary(
                String recipientEmail,
                String locale,
                int sessions,
                BigDecimal volume,
                int personalRecords,
                int streak) {
            recipients.add(recipientEmail);
        }

        List<String> recipients() {
            return List.copyOf(recipients);
        }

        void clear() {
            recipients.clear();
        }
    }
}
