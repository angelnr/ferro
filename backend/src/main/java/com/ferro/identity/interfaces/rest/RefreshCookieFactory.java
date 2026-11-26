package com.ferro.identity.interfaces.rest;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.ferro.identity.application.RefreshProperties;

/** Construye y limpia la cookie de refresh con la configuración de ADR-0006. */
@Component
public class RefreshCookieFactory {

    private final RefreshProperties properties;

    public RefreshCookieFactory(RefreshProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(String rawToken) {
        return base(rawToken).maxAge(properties.ttl()).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(RefreshProperties.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path("/api/v1/auth");
    }
}
