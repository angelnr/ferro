package com.ferro.identity.infrastructure.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ferro.identity.application.port.PasswordHasher;

/** Argon2id con los parámetros de ADR-0006 (2^16 KiB, 2 iteraciones, p=1). */
@Component
public class Argon2PasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 1 << 16, 2);

    @Override
    public String hash(CharSequence rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encodedPassword != null && encoder.matches(rawPassword, encodedPassword);
    }
}
