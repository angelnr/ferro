package com.ferro.identity.application;

public record UpdateProfileCommand(
        String displayName,
        String locale,
        String unitSystem,
        String theme,
        Integer defaultRestSeconds,
        Boolean timerSound,
        Boolean emailWeeklySummary) {}
