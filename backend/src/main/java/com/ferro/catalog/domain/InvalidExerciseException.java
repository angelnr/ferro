package com.ferro.catalog.domain;

import com.ferro.shared.domain.DomainException;

/** Datos de un ejercicio custom inválidos (422 VALIDATION_FAILED). */
public final class InvalidExerciseException extends DomainException {

    public InvalidExerciseException(String message) {
        super(message);
    }
}
