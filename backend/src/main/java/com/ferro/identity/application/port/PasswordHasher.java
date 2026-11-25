package com.ferro.identity.application.port;

/**
 * Puerto de salida para hashing de contraseñas (Argon2id en producción, ADR-0006).
 *
 * <p>El dominio y la aplicación nunca dependen de la librería criptográfica concreta.
 */
public interface PasswordHasher {

    String hash(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String encodedPassword);
}
