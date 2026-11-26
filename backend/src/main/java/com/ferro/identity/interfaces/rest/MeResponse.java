package com.ferro.identity.interfaces.rest;

import com.ferro.identity.domain.User;

public record MeResponse(UserResponse user, PreferencesResponse preferences) {

    public static MeResponse from(User user) {
        return new MeResponse(UserResponse.from(user), PreferencesResponse.from(user));
    }
}
