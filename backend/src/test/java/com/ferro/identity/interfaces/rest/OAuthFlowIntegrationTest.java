package com.ferro.identity.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import com.ferro.identity.application.oauth.OAuthClient;
import com.ferro.identity.application.oauth.OAuthClientRegistry;
import com.ferro.identity.domain.Email;
import com.ferro.identity.domain.OAuthProvider;
import com.ferro.identity.domain.OAuthUser;
import com.ferro.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(
        properties = {
            "ferro.oauth.google.client-id=test-google-client",
            "ferro.oauth.google.client-secret=test-google-secret",
            "ferro.oauth.google.redirect-uri=http://localhost:4200/auth/oauth/callback/google"
        })
@AutoConfigureMockMvc
class OAuthFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OAuthClientRegistry clients;

    @Test
    void exposes_google_authorize_url_and_links_a_verified_account() throws Exception {
        String email = "oauth" + UUID.randomUUID().toString().replace("-", "") + "@ferro.dev";
        OAuthClient google = mock(OAuthClient.class);
        when(google.fetchUser(eq("google-code"), anyString()))
                .thenReturn(new OAuthUser("google-subject", new Email(email), true, "OAuth Tester"));
        when(clients.clientFor(OAuthProvider.GOOGLE)).thenReturn(google);

        mockMvc.perform(get("/api/v1/auth/oauth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.google").value(true))
                .andExpect(jsonPath("$.github").value(false));

        MvcResult authorize = mockMvc.perform(get("/api/v1/auth/oauth/google/authorize-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").isNotEmpty())
                .andReturn();

        String url = JsonPath.read(authorize.getResponse().getContentAsString(), "$.url");
        assertThat(url).contains("accounts.google.com", "code_challenge_method=S256");
        String state =
                UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("state");

        MvcResult callback = mockMvc.perform(post("/api/v1/auth/oauth/google/callback")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"code":"google-code","state":"%s"}
                                """.formatted(state)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String token = JsonPath.read(callback.getResponse().getContentAsString(), "$.accessToken");
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void reports_oauth_not_configured_for_github() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth/github/authorize-url"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("OAUTH_NOT_CONFIGURED"));
    }

    @Test
    void rejects_a_state_for_a_different_provider() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oauth/github/callback")
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"code\",\"state\":\"not-a-state\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("OAUTH_FAILED"));
    }
}
