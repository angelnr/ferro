package com.ferro.identity.interfaces.rest;

import com.ferro.identity.domain.User;

public record UserResponse(
        String id,
        String displayName,
        String email,
        String status,
        String locale,
        String unitSystem,
        String theme,
        String timeZone,
        int defaultRestSeconds) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.id().toString(),
                user.displayName().value(),
                user.email().value(),
                user.status().name(),
                user.locale().code(),
                user.unitSystem().name(),
                user.theme().name(),
                user.timeZone(),
                user.defaultRestSeconds());
    }
}
