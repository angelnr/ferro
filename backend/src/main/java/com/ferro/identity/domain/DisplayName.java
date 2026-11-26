package com.ferro.identity.domain;

import java.text.Normalizer;

/**
 * Nombre visible del usuario: NFKC + trim exterior, conservando los espacios internos.
 *
 * <p>La regla está en RF-001.5: no se colapsan ni se recortan espacios internos.
 */
public record DisplayName(String value) {

    private static final int MAX_LENGTH = 80;

    public DisplayName {
        if (value == null) {
            throw new InvalidDisplayNameException("El nombre es obligatorio");
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).trim();
        if (normalized.isEmpty() || normalized.length() > MAX_LENGTH) {
            throw new InvalidDisplayNameException("El nombre debe tener entre 1 y 80 caracteres");
        }
        value = normalized;
    }

    @Override
    public String toString() {
        return value;
    }
}
