package com.ferro.identity.domain;

/** Proveedores OAuth soportados (ADR-0006). */
public enum OAuthProvider {
    GOOGLE,
    GITHUB;

    public static OAuthProvider fromPath(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new OAuthLoginException("Proveedor OAuth no soportado: " + value);
        }
    }
}
