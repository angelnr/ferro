package com.ferro.catalog.domain;

import com.ferro.shared.domain.DomainException;

/** Límite de ejercicios custom alcanzado (RF-022). */
public final class ExerciseLimitReachedException extends DomainException {

    public ExerciseLimitReachedException() {
        super("Has alcanzado el límite de ejercicios personalizados");
    }
}
