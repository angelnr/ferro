package com.ferro.body.interfaces.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.ferro.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "ferro.rate-limit.enabled=false")
@AutoConfigureMockMvc
class BodyFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;
    private String otherToken;
    private String measuredAt;

    @BeforeEach
    void setUp() throws Exception {
        token = register("body-user");
        otherToken = register("body-intruder");
        measuredAt = Instant.parse("2026-11-20T10:00:00Z").toString();
    }

    @Test
    void creates_lists_updates_and_deletes_measurements() throws Exception {
        String created = mockMvc.perform(post("/api/v1/body-measurements")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"measuredAt":"%s","weight":80.5,"waist":82,"notes":"Mañana"}
                                """.formatted(measuredAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weight").value(80.5))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(post("/api/v1/body-measurements")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{\"measuredAt\":\"" + measuredAt + "\",\"weight\":81}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEASUREMENT_EXISTS"))
                .andExpect(jsonPath("$.measurementId").value(id));

        mockMvc.perform(get("/api/v1/body-measurements").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/body-measurements/" + id).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/body-measurements/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"measuredAt":"%s","weight":80.0,"waist":81.5,"notes":"Editado"}
                                """.formatted(measuredAt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight").value(80.0))
                .andExpect(jsonPath("$.notes").value("Editado"));

        mockMvc.perform(get("/api/v1/body-measurements/stats").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight.length()").value(1))
                .andExpect(jsonPath("$.waist.length()").value(1));

        mockMvc.perform(delete("/api/v1/body-measurements/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/body-measurements").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void rejects_invalid_measurements() throws Exception {
        mockMvc.perform(post("/api/v1/body-measurements")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
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
