package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** Credenciales incorrectas; el mensaje es deliberadamente genérico (RF-002). */
public final class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Email o contraseña incorrectos");
    }
}
