package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** Token de verificación inválido, caducado o ya usado (400 VERIFICATION_INVALID). */
public final class VerificationInvalidException extends DomainException {

    public VerificationInvalidException() {
        super("El enlace de verificación no es válido o ha caducado");
    }
}
