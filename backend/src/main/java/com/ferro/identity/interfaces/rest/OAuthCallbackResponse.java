package com.ferro.identity.interfaces.rest;

import com.ferro.identity.application.AuthResult;

/** Respuesta de callback: sesión completa o necesidad de aportar email (GitHub privado). */
public record OAuthCallbackResponse(UserResponse user, String accessToken, boolean needsEmail, String onboardingToken) {

    public static OAuthCallbackResponse authenticated(AuthResult result) {
        return new OAuthCallbackResponse(UserResponse.from(result.user()), result.accessToken(), false, null);
    }

    public static OAuthCallbackResponse needsEmail(String onboardingToken) {
        return new OAuthCallbackResponse(null, null, true, onboardingToken);
    }
}
