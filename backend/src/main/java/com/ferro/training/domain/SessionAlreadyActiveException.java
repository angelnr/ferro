package com.ferro.training.domain;

import com.ferro.shared.domain.DomainException;

/** El usuario ya tiene una sesión ACTIVE (RN-08). */
public final class SessionAlreadyActiveException extends DomainException {

    private final java.util.UUID activeSessionId;

    public SessionAlreadyActiveException(java.util.UUID activeSessionId) {
        super("Ya hay una sesión activa");
        this.activeSessionId = activeSessionId;
    }

    public java.util.UUID activeSessionId() {
        return activeSessionId;
    }
}
