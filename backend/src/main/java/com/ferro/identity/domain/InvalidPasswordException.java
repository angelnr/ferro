package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** La contraseña no cumple la política RN-14. */
public final class InvalidPasswordException extends DomainException {

    public InvalidPasswordException(String message) {
        super(message);
    }
}
