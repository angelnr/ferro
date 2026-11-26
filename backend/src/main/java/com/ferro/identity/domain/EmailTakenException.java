package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** Ya existe una cuenta con ese email (409 EMAIL_TAKEN). */
public final class EmailTakenException extends DomainException {

    public EmailTakenException() {
        super("Ya existe una cuenta con ese email");
    }
}
