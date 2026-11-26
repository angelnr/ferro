package com.ferro.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Credenciales y redirect URIs de los proveedores OAuth. Vacío = proveedor deshabilitado. */
@ConfigurationProperties("ferro.oauth")
public record OAuthProperties(
        @DefaultValue Google google, @DefaultValue Github github) {

    public record Google(
            @DefaultValue("") String clientId,
            @DefaultValue("") String clientSecret,

            @DefaultValue("http://localhost:4200/auth/oauth/callback/google")
            String redirectUri,

            @DefaultValue("openid email profile") String scopes) {

        public boolean configured() {
            return !clientId.isBlank() && !clientSecret.isBlank();
        }
    }

    public record Github(
            @DefaultValue("") String clientId,
            @DefaultValue("") String clientSecret,

            @DefaultValue("http://localhost:4200/auth/oauth/callback/github")
            String redirectUri,

            @DefaultValue("read:user user:email") String scopes) {

        public boolean configured() {
            return !clientId.isBlank() && !clientSecret.isBlank();
        }
    }
}
