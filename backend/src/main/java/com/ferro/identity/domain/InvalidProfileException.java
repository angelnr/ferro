package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** Perfil o preferencias inválidas (422 VALIDATION_FAILED). */
public final class InvalidProfileException extends DomainException {

    public InvalidProfileException(String message) {
        super(message);
    }
}
