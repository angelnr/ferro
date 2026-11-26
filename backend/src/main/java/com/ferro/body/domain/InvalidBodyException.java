package com.ferro.body.domain;

import com.ferro.shared.domain.DomainException;

public final class InvalidBodyException extends DomainException {

    public InvalidBodyException(String message) {
        super(message);
    }
}
