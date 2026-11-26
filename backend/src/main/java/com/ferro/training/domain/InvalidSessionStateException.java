package com.ferro.training.domain;

import com.ferro.shared.domain.DomainException;

/** La sesión ya no está activa o la operación no aplica. */
public final class InvalidSessionStateException extends DomainException {

    public InvalidSessionStateException(String message) {
        super(message);
    }
}
