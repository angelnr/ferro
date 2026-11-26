package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** El intercambio OAuth falló o el provider devolvió datos inválidos. */
public final class OAuthLoginException extends DomainException {

    public OAuthLoginException(String message) {
        super(message);
    }
}
