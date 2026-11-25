package com.ferro.identity.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Email del usuario, siempre normalizado (NFKC + trim + minúsculas).
 *
 * <p>Única vía de entrada del email al dominio (docs/05 §6.5). Dos instancias con el mismo
 * email lógico son iguales aunque el texto original difiera en mayúsculas o Unicode.
 */
public record Email(String value) {

    private static final int MAX_LENGTH = 254;
    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public Email {
        if (value == null) {
            throw new InvalidEmailException("El email es obligatorio");
        }
        String normalized =
                Normalizer.normalize(value, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_LENGTH || !FORMAT.matcher(normalized).matches()) {
            throw new InvalidEmailException("Email no válido: " + value);
        }
        value = normalized;
    }

    @Override
    public String toString() {
        return value;
    }
}
