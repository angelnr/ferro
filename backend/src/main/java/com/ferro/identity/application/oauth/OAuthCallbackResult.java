package com.ferro.identity.application.oauth;

import com.ferro.identity.application.AuthResult;

/** Resultado de callback: sesión completa o token para que el usuario aporte su email. */
public record OAuthCallbackResult(AuthResult session, String onboardingToken) {

    public boolean needsEmail() {
        return session == null;
    }
}
