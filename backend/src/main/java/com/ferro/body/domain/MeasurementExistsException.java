package com.ferro.body.domain;

import java.util.UUID;

import com.ferro.shared.domain.DomainException;

public final class MeasurementExistsException extends DomainException {

    private final UUID measurementId;

    public MeasurementExistsException(UUID measurementId) {
        super("Ya hay una medición en esa fecha");
        this.measurementId = measurementId;
    }

    public UUID measurementId() {
        return measurementId;
    }
}
