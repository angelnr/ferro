package com.ferro.body.domain;

import com.ferro.shared.domain.DomainException;

public final class BodyNotFoundException extends DomainException {

    public BodyNotFoundException() {
        super("Medición no encontrada");
    }
}
