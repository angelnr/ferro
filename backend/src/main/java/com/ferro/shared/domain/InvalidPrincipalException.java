package com.ferro.shared.domain;

/** El principal autenticado no tiene un identificador utilizable. */
public final class InvalidPrincipalException extends DomainException {

    public InvalidPrincipalException() {
        super("Token de acceso no válido");
    }
}
