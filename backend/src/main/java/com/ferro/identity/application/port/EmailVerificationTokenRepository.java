package com.ferro.identity.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.ferro.identity.domain.EmailVerificationToken;

public interface EmailVerificationTokenRepository {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    EmailVerificationToken save(EmailVerificationToken token);

    void invalidateActiveForUser(UUID userId, Instant now);
}
