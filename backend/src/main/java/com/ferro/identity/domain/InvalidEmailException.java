package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** El email no cumple el formato o la longitud admitidos. */
public final class InvalidEmailException extends DomainException {

    public InvalidEmailException(String message) {
        super(message);
    }
}
