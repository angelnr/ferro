package com.ferro.training.domain;

import com.ferro.shared.domain.DomainException;

/** Sesión/entrada inexistente o ajena (404 anti-IDOR). */
public final class SessionNotFoundException extends DomainException {

    public SessionNotFoundException() {
        super("Sesión no encontrada");
    }
}
