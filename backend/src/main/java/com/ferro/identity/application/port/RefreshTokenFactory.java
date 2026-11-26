package com.ferro.identity.application.port;

import java.time.Instant;
import java.util.UUID;

import com.ferro.identity.domain.RefreshToken;

/** Genera el refresh opaco y su representación persistible (hash). */
public interface RefreshTokenFactory {

    IssuedRefreshToken create(UUID userId, Instant now);

    record IssuedRefreshToken(RefreshToken token, String rawToken) {}
}
