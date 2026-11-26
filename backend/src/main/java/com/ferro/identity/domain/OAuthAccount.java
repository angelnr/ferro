package com.ferro.identity.domain;

import java.time.Instant;
import java.util.UUID;

import com.ferro.shared.domain.UuidV7;

/** Vinculación entre una cuenta local y una cuenta de un proveedor OAuth (RN-17). */
public final class OAuthAccount {

    private final UUID id;
    private final UUID userId;
    private final OAuthProvider provider;
    private final String providerAccountId;
    private final Email emailAtLink;
    private final Instant createdAt;

    private OAuthAccount(
            UUID id,
            UUID userId,
            OAuthProvider provider,
            String providerAccountId,
            Email emailAtLink,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.providerAccountId = providerAccountId;
        this.emailAtLink = emailAtLink;
        this.createdAt = createdAt;
    }

    public static OAuthAccount link(
            UUID userId, OAuthProvider provider, String providerAccountId, Email emailAtLink, Instant now) {
        return new OAuthAccount(UuidV7.generate(), userId, provider, providerAccountId, emailAtLink, now);
    }

    public static OAuthAccount rehydrate(
            UUID id,
            UUID userId,
            OAuthProvider provider,
            String providerAccountId,
            Email emailAtLink,
            Instant createdAt) {
        return new OAuthAccount(id, userId, provider, providerAccountId, emailAtLink, createdAt);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public OAuthProvider provider() {
        return provider;
    }

    public String providerAccountId() {
        return providerAccountId;
    }

    public Email emailAtLink() {
        return emailAtLink;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
