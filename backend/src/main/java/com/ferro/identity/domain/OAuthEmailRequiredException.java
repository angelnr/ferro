package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** El proveedor no entrega un email verificado (p. ej. GitHub con email privado). */
public final class OAuthEmailRequiredException extends DomainException {

    public OAuthEmailRequiredException() {
        super("El proveedor no ha entregado un email verificado");
    }
}
