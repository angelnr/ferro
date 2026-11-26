package com.ferro.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.ferro.identity.domain.RefreshToken;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_id")
    private UUID replacedBy;

    protected RefreshTokenJpaEntity() {}

    private RefreshTokenJpaEntity(RefreshToken token) {
        this.id = token.id();
        this.userId = token.userId();
        this.tokenHash = token.tokenHash();
        this.expiresAt = token.expiresAt();
        this.revokedAt = token.revokedAt();
        this.replacedBy = token.replacedBy();
    }

    public static RefreshTokenJpaEntity from(RefreshToken token) {
        return new RefreshTokenJpaEntity(token);
    }

    public RefreshToken toDomain() {
        return RefreshToken.rehydrate(id, userId, tokenHash, expiresAt, revokedAt, replacedBy);
    }

    public UUID getId() {
        return id;
    }
}
