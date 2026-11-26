package com.ferro.identity.domain;

import java.util.regex.Pattern;

/**
 * Política de contraseñas RN-14: ≥ 10 caracteres, minúscula, mayúscula y dígito.
 *
 * <p>La lista de las 10 000 contraseñas más comunes se incorporará como recurso en el
 * siguiente incremento de M1; hoy esta clase concentra la parte determinista de la regla.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 128;

    private static final Pattern HAS_LOWER = Pattern.compile("[a-z]");
    private static final Pattern HAS_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");

    private PasswordPolicy() {}

    public static void validate(CharSequence rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw new InvalidPasswordException("La contraseña debe tener al menos 10 caracteres");
        }
        if (rawPassword.length() > MAX_LENGTH) {
            throw new InvalidPasswordException("La contraseña no puede superar 128 caracteres");
        }
        if (!HAS_LOWER.matcher(rawPassword).find()) {
            throw new InvalidPasswordException("La contraseña debe incluir una minúscula");
        }
        if (!HAS_UPPER.matcher(rawPassword).find()) {
            throw new InvalidPasswordException("La contraseña debe incluir una mayúscula");
        }
        if (!HAS_DIGIT.matcher(rawPassword).find()) {
            throw new InvalidPasswordException("La contraseña debe incluir un dígito");
        }
    }
}
