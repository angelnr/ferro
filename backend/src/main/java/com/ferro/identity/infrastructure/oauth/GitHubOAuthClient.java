package com.ferro.identity.infrastructure.oauth;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ferro.identity.application.OAuthProperties;
import com.ferro.identity.application.oauth.OAuthClient;
import com.ferro.identity.domain.Email;
import com.ferro.identity.domain.OAuthLoginException;
import com.ferro.identity.domain.OAuthProvider;
import com.ferro.identity.domain.OAuthUser;

@Component
class GitHubOAuthClient implements OAuthClient {

    private static final URI TOKEN_URI = URI.create("https://github.com/login/oauth/access_token");
    private static final URI USER_URI = URI.create("https://api.github.com/user");
    private static final URI EMAILS_URI = URI.create("https://api.github.com/user/emails");

    private final OAuthProperties.Github properties;
    private final RestClient rest = RestClient.create();

    GitHubOAuthClient(OAuthProperties properties) {
        this.properties = properties.github();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GITHUB;
    }

    @Override
    public OAuthUser fetchUser(String code, String codeVerifier) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", properties.clientId());
            form.add("client_secret", properties.clientSecret());
            form.add("code", code);
            form.add("code_verifier", codeVerifier);
            form.add("redirect_uri", properties.redirectUri());

            Map<String, Object> token = rest.post()
                    .uri(TOKEN_URI)
                    .header("Accept", "application/json")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            String accessToken = token == null ? null : (String) token.get("access_token");
            if (accessToken == null) {
                throw new OAuthLoginException("GitHub no devolvió access token");
            }

            Map<String, Object> user = rest.get()
                    .uri(USER_URI)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (user == null || user.get("id") == null) {
                throw new OAuthLoginException("GitHub no devolvió identidad");
            }

            List<Map<String, Object>> emails = rest.get()
                    .uri(EMAILS_URI)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            String emailValue = emails == null
                    ? null
                    : emails.stream()
                            .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                            .filter(email -> Boolean.TRUE.equals(email.get("primary")))
                            .map(email -> (String) email.get("email"))
                            .findFirst()
                            .orElseGet(() -> emails.stream()
                                    .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                                    .map(email -> (String) email.get("email"))
                                    .findFirst()
                                    .orElse(null));

            Email email = emailValue == null ? null : new Email(emailValue);
            String name = user.get("name") instanceof String value && !value.isBlank()
                    ? value
                    : String.valueOf(user.get("login"));
            return new OAuthUser(String.valueOf(user.get("id")), email, email != null, name);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new OAuthLoginException("No se pudo validar la cuenta de GitHub");
        }
    }
}
