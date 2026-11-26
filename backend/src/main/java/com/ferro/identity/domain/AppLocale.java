package com.ferro.identity.domain;

/** Idiomas soportados por la API y la UI (ADR-0008). */
public enum AppLocale {
    ES("es"),
    EN("en");

    private final String code;

    AppLocale(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static AppLocale fromCode(String code) {
        if (code == null) {
            return ES;
        }
        return switch (code.toLowerCase()) {
            case "es" -> ES;
            case "en" -> EN;
            default -> throw new InvalidLocaleException("Locale no soportado: " + code);
        };
    }
}
