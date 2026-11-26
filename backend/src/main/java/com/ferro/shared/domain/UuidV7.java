package com.ferro.shared.domain;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generador de UUID v7 (RFC 9562): 48 bits de timestamp en ms, versión 7 y 74 bits aleatorios.
 *
 * <p>Preferimos generarlos en la aplicación para poder tener el id antes del INSERT y mantener
 * el orden temporal que espera el esquema (doc 05 §6.4).
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {}

    public static UUID generate() {
        long timestamp = System.currentTimeMillis() & 0xFFFFFFFFFFFFL;
        long mostSignificant = (timestamp << 16) | 0x7000L | RANDOM.nextInt(0x1000);
        long leastSignificant = (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(mostSignificant, leastSignificant);
    }
}
