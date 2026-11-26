package com.ferro.progress.domain;

import com.ferro.shared.domain.DomainException;

public final class ProgressNotFoundException extends DomainException {

    public ProgressNotFoundException() {
        super("Recurso no encontrado");
    }
}
