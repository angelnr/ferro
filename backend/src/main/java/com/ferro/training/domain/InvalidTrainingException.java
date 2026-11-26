package com.ferro.training.domain;

import com.ferro.shared.domain.DomainException;

/** Entrada de sesión/serie inválida (422 VALIDATION_FAILED). */
public final class InvalidTrainingException extends DomainException {

    public InvalidTrainingException(String message) {
        super(message);
    }
}
