package com.ferro.platform.ratelimit;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.ferro.support.AbstractPostgresIntegrationTest;

@SpringBootTest(properties = "ferro.rate-limit.enabled=true")
@AutoConfigureMockMvc
class RateLimitIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "Ferro12345";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_is_limited_per_ip_and_email() throws Exception {
        String email = "rl" + UUID.randomUUID().toString().replace("-", "") + "@ferro.dev";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"displayName":"Rate","email":"%s","password":"%s","healthConsent":true}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());

        for (int attempt = 0; attempt < 10; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }
}
