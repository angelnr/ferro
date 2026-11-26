package com.ferro.identity.domain;

import com.ferro.shared.domain.DomainException;

/** El registro exige consentimiento explícito para datos de salud (RGPD art. 9). */
public final class HealthConsentRequiredException extends DomainException {

    public HealthConsentRequiredException() {
        super("Debes aceptar el consentimiento de datos de salud");
    }
}
