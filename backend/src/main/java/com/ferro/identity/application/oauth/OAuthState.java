package com.ferro.identity.application.oauth;

import java.time.Instant;

import com.ferro.identity.domain.OAuthProvider;

public record OAuthState(OAuthProvider provider, String codeVerifier, String redirectUri, Instant expiresAt) {}
