package com.ferro.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.ferro.shared.domain.UuidV7;

/** Token de verificación de email de un solo uso (RN-16). */
public final class EmailVerificationToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant usedAt;

    private EmailVerificationToken(
            UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant usedAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }

    public static EmailVerificationToken issue(UUID userId, String tokenHash, Instant now, Duration ttl) {
        return new EmailVerificationToken(UuidV7.generate(), userId, tokenHash, now.plus(ttl), null, now);
    }

    public static EmailVerificationToken rehydrate(
            UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant usedAt, Instant createdAt) {
        return new EmailVerificationToken(id, userId, tokenHash, expiresAt, usedAt, createdAt);
    }

    public boolean isActive(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
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

    public Instant usedAt() {
        return usedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
