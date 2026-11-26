package com.ferro.identity.application;

public record RegisterCommand(
        String displayName,
        String email,
        String rawPassword,
        boolean healthConsent,
        String locale,
        String unitSystem) {}
