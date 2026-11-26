package com.ferro.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ferro.identity.domain.Email;
import com.ferro.identity.domain.OAuthAccount;
import com.ferro.identity.domain.OAuthProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "oauth_accounts")
class OAuthAccountJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private OAuthProvider provider;

    @Column(name = "provider_account_id", nullable = false, length = 255)
    private String providerAccountId;

    @Column(name = "email_at_link")
    private String emailAtLink;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OAuthAccountJpaEntity() {}

    private OAuthAccountJpaEntity(OAuthAccount account) {
        this.id = account.id();
        this.userId = account.userId();
        this.provider = account.provider();
        this.providerAccountId = account.providerAccountId();
        this.emailAtLink =
                account.emailAtLink() == null ? null : account.emailAtLink().value();
        this.createdAt = account.createdAt();
    }

    static OAuthAccountJpaEntity from(OAuthAccount account) {
        return new OAuthAccountJpaEntity(account);
    }

    OAuthAccount toDomain() {
        return OAuthAccount.rehydrate(
                id,
                userId,
                provider,
                providerAccountId,
                emailAtLink == null ? null : new Email(emailAtLink),
                createdAt);
    }
}
