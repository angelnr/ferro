package com.ferro.identity.application.oauth;

import com.ferro.identity.domain.OAuthProvider;

public record OAuthOnboarding(OAuthProvider provider, String providerSubject, String displayName) {}
