package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** Token de recuperación inválido, caducado o ya usado (400 PASSWORD_RESET_INVALID). */
public final class PasswordResetInvalidException extends DomainException {

    public PasswordResetInvalidException() {
        super("El enlace de recuperación no es válido o ha caducado");
    }
}
