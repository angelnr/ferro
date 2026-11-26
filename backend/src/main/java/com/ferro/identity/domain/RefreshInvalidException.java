package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** El refresh token no existe, ha expirado o fue revocado (401 REFRESH_INVALID). */
public final class RefreshInvalidException extends DomainException {

    public RefreshInvalidException() {
        super("Refresh token no válido");
    }
}
