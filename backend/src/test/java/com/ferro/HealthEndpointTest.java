package com.ferro;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.ferro.support.AbstractPostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsUpWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void prometheusEndpointIsExposed() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
    }

    @Test
    void cors_allows_both_local_frontend_origins() throws Exception {
        for (String origin : new String[] {"http://localhost:4200", "http://127.0.0.1:4200"}) {
            mockMvc.perform(options("/api/v1/auth/login")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin))
                    .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        }
    }
}
