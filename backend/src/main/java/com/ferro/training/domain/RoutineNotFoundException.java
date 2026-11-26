package com.ferro.training.domain;

import com.ferro.shared.domain.DomainException;

/** Rutina inexistente o ajena (404 anti-IDOR). */
public final class RoutineNotFoundException extends DomainException {

    public RoutineNotFoundException() {
        super("Rutina no encontrada");
    }
}
