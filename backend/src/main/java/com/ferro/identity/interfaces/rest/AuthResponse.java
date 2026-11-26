package com.ferro.identity.interfaces.rest;

import com.ferro.identity.application.AuthResult;

public record AuthResponse(UserResponse user, String accessToken) {

    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(UserResponse.from(result.user()), result.accessToken());
    }
}
