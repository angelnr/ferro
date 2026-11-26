package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** El locale solicitado no está soportado (es/en). */
public final class InvalidLocaleException extends DomainException {

    public InvalidLocaleException(String message) {
        super(message);
    }
}
