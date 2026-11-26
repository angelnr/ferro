package com.ferro.identity.application;

import com.ferro.identity.domain.User;

/** Resultado interno de un registro/login/refresh; el refresh solo sale hacia la cookie. */
public record AuthResult(User user, String accessToken, String refreshToken) {}
