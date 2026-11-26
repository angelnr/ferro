package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** Proveedor OAuth sin client-id/client-secret configurados. */
public final class OAuthNotConfiguredException extends DomainException {

    public OAuthNotConfiguredException(String provider) {
        super("OAuth no configurado para " + provider);
    }
}
