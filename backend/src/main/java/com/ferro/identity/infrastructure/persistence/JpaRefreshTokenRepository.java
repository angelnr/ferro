package com.ferro.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.ferro.identity.application.port.RefreshTokenRepository;
import com.ferro.identity.domain.RefreshToken;

@Repository
class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository tokens;

    JpaRefreshTokenRepository(RefreshTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return tokens.findByTokenHash(tokenHash).map(RefreshTokenJpaEntity::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        tokens.save(RefreshTokenJpaEntity.from(token));
        return token;
    }

    @Override
    public void revokeAllForUser(UUID userId, Instant now) {
        tokens.revokeAllForUser(userId, now);
    }

    @Override
    public void revokeAllForUserExcept(UUID userId, String keepTokenHash, Instant now) {
        tokens.revokeAllForUserExcept(userId, keepTokenHash, now);
    }
}
