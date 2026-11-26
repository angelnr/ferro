package com.ferro.identity.application.port;

import java.time.Instant;
import java.util.UUID;

/** Emite el access token JWT (HS256, 15 min por defecto). */
public interface AccessTokenIssuer {

    String issue(UUID userId, Instant now);
}
