package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** El sistema de unidades solicitado no está soportado (KG/LB). */
public final class InvalidUnitSystemException extends DomainException {

    public InvalidUnitSystemException(String message) {
        super(message);
    }
}
