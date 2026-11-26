package com.ferro.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.ferro.shared.domain.UuidV7;

/**
 * Refresh token opaco. En BD solo se guarda su SHA-256; el valor en claro nunca se persiste.
 *
 * <p>La rotación encadena tokens con {@code replacedBy}; presentar uno revocado activa la
 * detección de robo y revoca toda la familia (RN-15).
 */
public final class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant revokedAt;
    private UUID replacedBy;

    private RefreshToken(
            UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant revokedAt, UUID replacedBy) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.replacedBy = replacedBy;
    }

    public static RefreshToken issue(UUID userId, String tokenHash, Instant now, Duration ttl) {
        return new RefreshToken(UuidV7.generate(), userId, tokenHash, now.plus(ttl), null, null);
    }

    public static RefreshToken rehydrate(
            UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant revokedAt, UUID replacedBy) {
        return new RefreshToken(id, userId, tokenHash, expiresAt, revokedAt, replacedBy);
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(Instant now, UUID replacementId) {
        this.revokedAt = now;
        this.replacedBy = replacementId;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public UUID replacedBy() {
        return replacedBy;
    }
}
