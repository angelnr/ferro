package com.ferro.identity.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de la cookie de refresh. En local va sin Secure y con SameSite=Lax; en
 * producción el perfil correspondiente fuerza Secure + SameSite=None (ADR-0006).
 */
@ConfigurationProperties("ferro.security.refresh")
public record RefreshProperties(
        @DefaultValue("30d") Duration ttl,
        @DefaultValue("false") boolean cookieSecure,
        @DefaultValue("Lax") String cookieSameSite) {

    public static final String COOKIE_NAME = "ferro_refresh";
}
