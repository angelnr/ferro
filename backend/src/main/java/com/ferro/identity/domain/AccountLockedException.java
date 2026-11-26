package com.ferro.identity.domain;

import java.time.Instant;

import com.ferro.shared.domain.DomainException;

/** La cuenta está bloqueada temporalmente por intentos fallidos (RF-002.2). */
public final class AccountLockedException extends DomainException {

    private final Instant lockedUntil;

    public AccountLockedException(Instant lockedUntil) {
        super("La cuenta está bloqueada temporalmente");
        this.lockedUntil = lockedUntil;
    }

    public Instant lockedUntil() {
        return lockedUntil;
    }
}
