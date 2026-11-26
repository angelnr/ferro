package com.ferro.catalog.domain;

import com.ferro.shared.domain.DomainException;

/** Filtros o paginación inválidos (400 INVALID_FILTER). */
public final class InvalidFilterException extends DomainException {

    public InvalidFilterException(String message) {
        super(message);
    }
}
