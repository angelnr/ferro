package com.ferro.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.ferro.support.AbstractPostgresIntegrationTest;

@SpringBootTest(properties = {"ferro.demo.enabled=true", "ferro.rate-limit.enabled=false"})
@AutoConfigureMockMvc
class DemoDataSeederIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void creates_demo_account_with_routines_and_twelve_weeks() throws Exception {
        Integer routines = jdbc.queryForObject(
                "SELECT count(*) FROM routines r JOIN users u ON u.id = r.user_id WHERE u.email = 'demo@ferro.app'",
                Integer.class);
        Integer sessions = jdbc.queryForObject(
                "SELECT count(*) FROM workout_sessions ws JOIN users u ON u.id = ws.user_id WHERE u.email = 'demo@ferro.app'",
                Integer.class);
        assertThat(routines).isEqualTo(3);
        assertThat(sessions).isEqualTo(36);

        mockMvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content("""
                                {"email":"demo@ferro.app","password":"FerroDemo26"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("demo@ferro.app"))
                .andExpect(jsonPath("$.user.displayName").value("Ferro Demo"));
    }
}
