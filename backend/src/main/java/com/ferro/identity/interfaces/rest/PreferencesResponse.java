package com.ferro.identity.interfaces.rest;

import com.ferro.identity.domain.User;

public record PreferencesResponse(
        String locale, String unitSystem, String theme, String timeZone, int defaultRestSeconds, boolean timerSound) {

    public static PreferencesResponse from(User user) {
        return new PreferencesResponse(
                user.locale().code(),
                user.unitSystem().name(),
                user.theme().name(),
                user.timeZone(),
                user.defaultRestSeconds(),
                user.timerSound());
    }
}
