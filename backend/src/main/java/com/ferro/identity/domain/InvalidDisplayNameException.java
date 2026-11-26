package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** El nombre visible no es válido (vacío o > 80 caracteres tras normalizar). */
public final class InvalidDisplayNameException extends DomainException {

    public InvalidDisplayNameException(String message) {
        super(message);
    }
}
