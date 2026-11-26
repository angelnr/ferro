package com.ferro.identity.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
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
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.ferro.identity.application.port.EmailSender;
import com.ferro.identity.domain.Email;
import com.ferro.support.AbstractPostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthFlowIntegrationTest.EmailTestConfig.class)
class AuthFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "Ferro12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CapturingEmailSender emailSender;

    @BeforeEach
    void clearEmails() {
        emailSender.clear();
    }

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

    @Test
    void register_sends_verification_email_and_confirm_activates_user() throws Exception {
        Registered registered = register(uniqueEmail());

        assertThat(emailSender.emails()).hasSize(1);
        String token = tokenFrom(emailSender.last().link());

        mockMvc.perform(post("/api/v1/auth/email-verification/confirm")
                        .contentType(APPLICATION_JSON)
                        .content(confirmBody(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + registered.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/auth/email-verification/confirm")
                        .contentType(APPLICATION_JSON)
                        .content(confirmBody(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_INVALID"));
    }

    @Test
    void resend_invalidates_the_previous_token() throws Exception {
        Registered registered = register(uniqueEmail());
        String firstToken = tokenFrom(emailSender.last().link());

        mockMvc.perform(post("/api/v1/auth/email-verification/resend")
                        .header("Authorization", "Bearer " + registered.accessToken()))
                .andExpect(status().isAccepted());

        assertThat(emailSender.emails()).hasSize(2);
        String secondToken = tokenFrom(emailSender.last().link());
        assertThat(secondToken).isNotEqualTo(firstToken);

        mockMvc.perform(post("/api/v1/auth/email-verification/confirm")
                        .contentType(APPLICATION_JSON)
                        .content(confirmBody(firstToken)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/email-verification/confirm")
                        .contentType(APPLICATION_JSON)
                        .content(confirmBody(secondToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalid_verification_token_is_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/email-verification/confirm")
                        .contentType(APPLICATION_JSON)
                        .content(confirmBody("not-a-real-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFICATION_INVALID"));
    }

    @Test
    void openapi_docs_are_public_and_include_auth_paths() throws Exception {
        mockMvc.perform(get("/api/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Ferro API"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/register']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/email-verification/confirm']")
                        .exists());
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

    private String confirmBody(String token) {
        return "{\"token\": \"%s\"}".formatted(token);
    }

    private String tokenFrom(String link) {
        String query = URI.create(link).getRawQuery();
        for (String parameter : query.split("&")) {
            if (parameter.startsWith("token=")) {
                return parameter.substring("token=".length());
            }
        }
        throw new IllegalStateException("El enlace no contiene token: " + link);
    }

    private record Registered(String email, String accessToken, Cookie cookie) {}

    @TestConfiguration
    static class EmailTestConfig {

        @Bean
        @Primary
        CapturingEmailSender capturingEmailSender() {
            return new CapturingEmailSender();
        }
    }

    static class CapturingEmailSender implements EmailSender {

        private final List<SentEmail> emails = new CopyOnWriteArrayList<>();

        @Override
        public void sendVerificationEmail(Email recipient, String verificationLink) {
            emails.add(new SentEmail(recipient, verificationLink));
        }

        List<SentEmail> emails() {
            return List.copyOf(emails);
        }

        SentEmail last() {
            return emails.get(emails.size() - 1);
        }

        void clear() {
            emails.clear();
        }
    }

    record SentEmail(Email recipient, String link) {}
}
