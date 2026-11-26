package com.ferro.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.ferro.identity.domain.EmailVerificationToken;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmailVerificationTokenJpaEntity() {}

    private EmailVerificationTokenJpaEntity(EmailVerificationToken token) {
        this.id = token.id();
        this.userId = token.userId();
        this.tokenHash = token.tokenHash();
        this.expiresAt = token.expiresAt();
        this.usedAt = token.usedAt();
        this.createdAt = token.createdAt();
    }

    public static EmailVerificationTokenJpaEntity from(EmailVerificationToken token) {
        return new EmailVerificationTokenJpaEntity(token);
    }

    public EmailVerificationToken toDomain() {
        return EmailVerificationToken.rehydrate(id, userId, tokenHash, expiresAt, usedAt, createdAt);
    }
}
