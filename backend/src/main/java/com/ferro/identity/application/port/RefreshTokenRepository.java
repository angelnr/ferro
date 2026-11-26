package com.ferro.identity.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.ferro.identity.domain.RefreshToken;

/** Puerto de salida de refresh tokens. */
public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken token);

    void revokeAllForUser(UUID userId, Instant now);

    void revokeAllForUserExcept(UUID userId, String keepTokenHash, Instant now);
}
