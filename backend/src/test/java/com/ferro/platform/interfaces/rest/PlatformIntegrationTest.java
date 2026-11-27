package com.ferro.platform.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.ferro.support.AbstractPostgresIntegrationTest;

@SpringBootTest(properties = "ferro.rate-limit.enabled=false")
@AutoConfigureMockMvc
class PlatformIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposes_public_flags_and_correlation_id() throws Exception {
        mockMvc.perform(get("/api/v1/config/flags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key == 'demo-account')]").exists())
                .andExpect(header().exists("X-Correlation-Id"));
    }
}
