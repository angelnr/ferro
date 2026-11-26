package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** El header Origin no está en la allowlist CORS/CSRF (ADR-0006). */
public final class InvalidOriginException extends DomainException {

    public InvalidOriginException() {
        super("Origen no permitido");
    }
}
