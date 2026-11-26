package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** El email no se ha verificado dentro de las 24 h (RN-16). */
public final class EmailUnverifiedException extends DomainException {

    public EmailUnverifiedException() {
        super("Debes verificar tu email para iniciar sesión");
    }
}
