package com.ferro.identity.domain;

/** Identidad normalizada que devuelve un proveedor OAuth. */
public record OAuthUser(String subject, Email email, boolean emailVerified, String displayName) {}
