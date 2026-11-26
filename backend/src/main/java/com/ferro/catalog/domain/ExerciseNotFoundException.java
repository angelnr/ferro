package com.ferro.catalog.domain;

import com.ferro.shared.domain.DomainException;

/** Ejercicio inexistente o ajeno (404, anti-IDOR). */
public final class ExerciseNotFoundException extends DomainException {

    public ExerciseNotFoundException() {
        super("Ejercicio no encontrado");
    }
}
