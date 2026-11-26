package com.ferro.catalog.domain;

import com.ferro.shared.domain.DomainException;

/** Un ejercicio custom con historial solo puede archivarse (RF-023). */
public final class ExerciseInUseException extends DomainException {

    public ExerciseInUseException() {
        super("El ejercicio tiene historial: solo puedes archivarlo");
    }
}
