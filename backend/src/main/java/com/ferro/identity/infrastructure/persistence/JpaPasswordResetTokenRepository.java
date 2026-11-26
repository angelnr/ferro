package com.ferro.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.ferro.identity.application.port.PasswordResetTokenRepository;
import com.ferro.identity.domain.PasswordResetToken;

@Repository
class JpaPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository tokens;

    JpaPasswordResetTokenRepository(PasswordResetTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return tokens.findByTokenHash(tokenHash).map(PasswordResetTokenJpaEntity::toDomain);
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        tokens.save(PasswordResetTokenJpaEntity.from(token));
        return token;
    }

    @Override
    public void invalidateActiveForUser(UUID userId, Instant now) {
        tokens.invalidateActiveForUser(userId, now);
    }
}
