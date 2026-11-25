package com.ferro.shared.domain;

/**
 * Error de negocio conocido y recuperable.
 *
 * <p>Los adaptadores lo traducen a la respuesta de API adecuada (RFC 7807); el dominio nunca conoce HTTP.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
