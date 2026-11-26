package com.ferro.identity.infrastructure.oauth;

import java.net.URI;
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
class GoogleOAuthClient implements OAuthClient {

    private static final URI TOKEN_URI = URI.create("https://oauth2.googleapis.com/token");
    private static final URI USERINFO_URI = URI.create("https://openidconnect.googleapis.com/v1/userinfo");

    private final OAuthProperties.Google properties;
    private final RestClient rest = RestClient.create();

    GoogleOAuthClient(OAuthProperties properties) {
        this.properties = properties.google();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUser fetchUser(String code, String codeVerifier) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("client_id", properties.clientId());
            form.add("client_secret", properties.clientSecret());
            form.add("code", code);
            form.add("code_verifier", codeVerifier);
            form.add("redirect_uri", properties.redirectUri());

            Map<String, Object> token = rest.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            String accessToken = token == null ? null : (String) token.get("access_token");
            if (accessToken == null) {
                throw new OAuthLoginException("Google no devolvió access token");
            }

            Map<String, Object> user = rest.get()
                    .uri(USERINFO_URI)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (user == null || user.get("sub") == null) {
                throw new OAuthLoginException("Google no devolvió identidad");
            }

            String rawEmail = (String) user.get("email");
            Email email = rawEmail == null ? null : new Email(rawEmail);
            String name = user.get("name") instanceof String value ? value : rawEmail;
            return new OAuthUser(
                    String.valueOf(user.get("sub")), email, Boolean.TRUE.equals(user.get("email_verified")), name);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new OAuthLoginException("No se pudo validar la cuenta de Google");
        }
    }
}
