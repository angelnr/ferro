package com.ferro.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.ferro.identity.application.port.EmailVerificationTokenRepository;
import com.ferro.identity.domain.EmailVerificationToken;

@Repository
class JpaEmailVerificationTokenRepository implements EmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository tokens;

    JpaEmailVerificationTokenRepository(EmailVerificationTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public Optional<EmailVerificationToken> findByTokenHash(String tokenHash) {
        return tokens.findByTokenHash(tokenHash).map(EmailVerificationTokenJpaEntity::toDomain);
    }

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        tokens.save(EmailVerificationTokenJpaEntity.from(token));
        return token;
    }

    @Override
    public void invalidateActiveForUser(UUID userId, Instant now) {
        tokens.invalidateActiveForUser(userId, now);
    }
}
