package com.ferro.identity.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.ferro.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "Ferro12345";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void register_returns_user_token_and_refresh_cookie() throws Exception {
        Registered registered = register(uniqueEmail());

        assertThat(registered.accessToken()).isNotBlank();
        assertThat(registered.cookie()).isNotNull();
        assertThat(registered.cookie().isHttpOnly()).isTrue();
        assertThat(registered.cookie().getPath()).isEqualTo("/api/v1/auth");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + registered.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(registered.email()))
                .andExpect(jsonPath("$.user.status").value("UNVERIFIED"))
                .andExpect(jsonPath("$.preferences.locale").value("es"))
                .andExpect(jsonPath("$.preferences.theme").value("DARK"));
    }

    @Test
    void register_rejects_duplicate_email_case_insensitive() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerBody(email.toUpperCase(), PASSWORD, true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void register_rejects_weak_password_with_field_errors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerBody(uniqueEmail(), "short", true)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.password").isArray());
    }

    @Test
    void register_requires_health_consent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerBody(uniqueEmail(), PASSWORD, false)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void login_works_and_rejects_wrong_password() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(loginBody(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody(email, "WrongPass123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refresh_rotates_and_reusing_a_revoked_token_revokes_the_family() throws Exception {
        Registered registered = register(uniqueEmail());

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(registered.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie newCookie = refreshed.getResponse().getCookie("ferro_refresh");
        assertThat(newCookie).isNotNull();
        assertThat(newCookie.getValue()).isNotEqualTo(registered.cookie().getValue());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(registered.cookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_INVALID"));

        // La reutilización revoca toda la familia, incluido el refresh recién emitido.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(newCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_INVALID"));
    }

    @Test
    void logout_revokes_refresh_and_clears_cookie() throws Exception {
        Registered registered = register(uniqueEmail());

        MvcResult logout = mockMvc.perform(post("/api/v1/auth/logout").cookie(registered.cookie()))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie cleared = logout.getResponse().getCookie("ferro_refresh");
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(registered.cookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_INVALID"));
    }

    @Test
    void refresh_without_cookie_is_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_INVALID"));
    }

    @Test
    void refresh_with_disallowed_origin_is_rejected() throws Exception {
        Registered registered = register(uniqueEmail());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(registered.cookie())
                        .header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void account_locks_after_five_consecutive_failures() throws Exception {
        String email = uniqueEmail();
        register(email);

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(loginBody(email, "WrongPass123")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(loginBody(email, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    private Registered register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerBody(email, PASSWORD, true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        Cookie cookie = result.getResponse().getCookie("ferro_refresh");
        return new Registered(email, JsonPath.read(body, "$.accessToken"), cookie);
    }

    private String registerBody(String email, String password, boolean consent) {
        return """
                {
                  "displayName": "Ángel",
                  "email": "%s",
                  "password": "%s",
                  "healthConsent": %s,
                  "locale": "es",
                  "unitSystem": "KG"
                }
                """.formatted(email, password, consent);
    }

    private String loginBody(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }

    private String uniqueEmail() {
        return "u" + UUID.randomUUID().toString().replace("-", "") + "@ferro.dev";
    }

    private record Registered(String email, String accessToken, Cookie cookie) {}
}
