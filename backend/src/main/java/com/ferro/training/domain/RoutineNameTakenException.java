package com.ferro.training.domain;

import com.ferro.shared.domain.DomainException;

/** Nombre de rutina duplicado para el mismo usuario (409 ROUTINE_NAME_TAKEN). */
public final class RoutineNameTakenException extends DomainException {

    public RoutineNameTakenException() {
        super("Ya tienes una rutina con ese nombre");
    }
}
