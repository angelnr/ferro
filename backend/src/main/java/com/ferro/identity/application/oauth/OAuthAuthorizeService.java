package com.ferro.identity.application.oauth;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.ferro.identity.application.OAuthProperties;
import com.ferro.identity.domain.OAuthNotConfiguredException;
import com.ferro.identity.domain.OAuthProvider;

/** Construye la URL de autorización con PKCE y state firmado. */
@Service
public class OAuthAuthorizeService {

    private final OAuthProperties properties;
    private final OAuthStateService stateService;

    public OAuthAuthorizeService(OAuthProperties properties, OAuthStateService stateService) {
        this.properties = properties;
        this.stateService = stateService;
    }

    public Map<String, Boolean> enabledProviders() {
        Map<String, Boolean> providers = new LinkedHashMap<>();
        providers.put("google", properties.google().configured());
        providers.put("github", properties.github().configured());
        return providers;
    }

    public String authorizeUrl(OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> googleUrl();
            case GITHUB -> githubUrl();
        };
    }

    private String googleUrl() {
        OAuthProperties.Google google = properties.google();
        if (!google.configured()) {
            throw new OAuthNotConfiguredException("google");
        }
        Pkce pkce = Pkce.generate();
        String state = stateService.create(OAuthProvider.GOOGLE, pkce.verifier(), google.redirectUri());
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", google.clientId())
                .queryParam("redirect_uri", google.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", google.scopes())
                .queryParam("state", state)
                .queryParam("code_challenge", pkce.challenge())
                .queryParam("code_challenge_method", "S256")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .encode()
                .toUriString();
    }

    private String githubUrl() {
        OAuthProperties.Github github = properties.github();
        if (!github.configured()) {
            throw new OAuthNotConfiguredException("github");
        }
        Pkce pkce = Pkce.generate();
        String state = stateService.create(OAuthProvider.GITHUB, pkce.verifier(), github.redirectUri());
        return UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", github.clientId())
                .queryParam("redirect_uri", github.redirectUri())
                .queryParam("scope", github.scopes())
                .queryParam("state", state)
                .queryParam("code_challenge", pkce.challenge())
                .queryParam("code_challenge_method", "S256")
                .queryParam("allow_signup", "true")
                .build()
                .encode()
                .toUriString();
    }
}
