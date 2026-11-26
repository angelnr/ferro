package com.ferro.identity.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.ferro.identity.domain.PasswordResetToken;

public interface PasswordResetTokenRepository {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    PasswordResetToken save(PasswordResetToken token);

    void invalidateActiveForUser(UUID userId, Instant now);
}
